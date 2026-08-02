package com.memorydrawer.memorydraft.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalOriginalImageStorage implements OriginalImageStorage {

	private final Path root;

	public LocalOriginalImageStorage(
		@Value("${memory-drawer.storage.root}") String root
	) {
		this.root = Path.of(root).toAbsolutePath().normalize();
	}

	@Override
	public StoredImage store(UUID ownerId, UUID draftId, ValidatedImage image) {
		String key = "drafts/%s/%s/original.%s".formatted(ownerId, draftId, image.extension());
		Path target = resolveSafely(key);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, image.bytes(), StandardOpenOption.CREATE_NEW);
			return new StoredImage(key, image.contentType());
		} catch (IOException exception) {
			throw new IllegalStateException("원본 이미지를 저장할 수 없습니다.", exception);
		}
	}

	@Override
	public byte[] load(String key) {
		try {
			return Files.readAllBytes(resolveSafely(key));
		} catch (IOException exception) {
			throw new IllegalStateException("원본 이미지를 읽을 수 없습니다.", exception);
		}
	}

	@Override
	public void delete(String key) {
		Path target = resolveSafely(key);
		try {
			Files.deleteIfExists(target);
			deleteEmptyParents(target.getParent());
		} catch (IOException exception) {
			throw new IllegalStateException("원본 이미지를 삭제할 수 없습니다.", exception);
		}
	}

	private Path resolveSafely(String key) {
		Path resolved = root.resolve(key).normalize();
		if (!resolved.startsWith(root)) {
			throw new IllegalArgumentException("허용되지 않는 이미지 저장 키입니다.");
		}
		return resolved;
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
}
