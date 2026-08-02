package com.memorydrawer.memorydraft.image;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalLetterFrontImageStorage implements LetterFrontImageStorage {

	private static final byte[] PNG_SIGNATURE = new byte[] {
		(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
	};

	private final Path root;

	public LocalLetterFrontImageStorage(
		@Value("${memory-drawer.storage.root}") String root
	) {
		this.root = Path.of(root).toAbsolutePath().normalize();
	}

	@Override
	public void store(UUID ownerId, UUID draftId, byte[] pngBytes) {
		if (!isPng(pngBytes)) {
			throw new IllegalArgumentException("손편지 앞면 결과는 PNG 형식이어야 합니다.");
		}
		Path target = resolveSafely(ownerId, draftId);
		Path temporary = null;
		try {
			Files.createDirectories(target.getParent());
			temporary = Files.createTempFile(target.getParent(), "letter-front-", ".tmp");
			Files.write(temporary, pngBytes, StandardOpenOption.TRUNCATE_EXISTING);
			moveReplacing(temporary, target);
		} catch (IOException exception) {
			throw new IllegalStateException("손편지 배경 제거 결과를 저장할 수 없습니다.", exception);
		} finally {
			deleteTemporary(temporary);
		}
	}

	@Override
	public byte[] load(UUID ownerId, UUID draftId) {
		try {
			return Files.readAllBytes(resolveSafely(ownerId, draftId));
		} catch (IOException exception) {
			throw new IllegalStateException("손편지 배경 제거 결과를 읽을 수 없습니다.", exception);
		}
	}

	@Override
	public void delete(UUID ownerId, UUID draftId) {
		Path target = resolveSafely(ownerId, draftId);
		try {
			Files.deleteIfExists(target);
			deleteEmptyParents(target.getParent());
		} catch (IOException exception) {
			throw new IllegalStateException("손편지 배경 제거 결과를 삭제할 수 없습니다.", exception);
		}
	}

	private Path resolveSafely(UUID ownerId, UUID draftId) {
		String key = "drafts/%s/%s/letter-front.png".formatted(ownerId, draftId);
		Path resolved = root.resolve(key).normalize();
		if (!resolved.startsWith(root)) {
			throw new IllegalArgumentException("허용되지 않는 손편지 이미지 저장 경로입니다.");
		}
		return resolved;
	}

	private void moveReplacing(Path source, Path target) throws IOException {
		try {
			Files.move(
				source,
				target,
				StandardCopyOption.ATOMIC_MOVE,
				StandardCopyOption.REPLACE_EXISTING
			);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void deleteTemporary(Path temporary) {
		if (temporary == null) {
			return;
		}
		try {
			Files.deleteIfExists(temporary);
		} catch (IOException ignored) {
			// A failed temporary-file cleanup must not replace the original storage failure.
		}
	}

	private void deleteEmptyParents(Path directory) throws IOException {
		Path draftsRoot = root.resolve("drafts").normalize();
		Path current = directory;
		while (current != null && current.startsWith(draftsRoot) && !current.equals(draftsRoot)) {
			try (var entries = Files.list(current)) {
				if (entries.findAny().isPresent()) {
					return;
				}
			}
			Files.deleteIfExists(current);
			current = current.getParent();
		}
	}

	private boolean isPng(byte[] bytes) {
		if (bytes == null || bytes.length < PNG_SIGNATURE.length) {
			return false;
		}
		for (int index = 0; index < PNG_SIGNATURE.length; index++) {
			if (bytes[index] != PNG_SIGNATURE[index]) {
				return false;
			}
		}
		return true;
	}
}
