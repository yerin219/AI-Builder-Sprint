package com.memorydrawer.card.image;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.memorydrawer.memorydraft.image.ValidatedImage;

@Component
public class LocalBackPhotoStorage implements BackPhotoStorage {

	private final Path root;

	public LocalBackPhotoStorage(@Value("${memory-drawer.storage.root}") String root) {
		this.root = Path.of(root).toAbsolutePath().normalize();
	}

	@Override
	public List<String> store(UUID ownerId, UUID cardId, List<ValidatedImage> images) {
		List<String> storedKeys = new ArrayList<>();
		try {
			for (int index = 0; index < images.size(); index++) {
				ValidatedImage image = images.get(index);
				String key = "cards/%s/%s/back/%d.%s".formatted(
					ownerId,
					cardId,
					index + 1,
					image.extension()
				);
				Path target = resolveSafely(key);
				Files.createDirectories(target.getParent());
				try (OutputStream output = Files.newOutputStream(
					target,
					StandardOpenOption.CREATE_NEW,
					StandardOpenOption.WRITE
				)) {
					storedKeys.add(key);
					output.write(image.bytes());
				}
			}
			return List.copyOf(storedKeys);
		} catch (IOException exception) {
			deleteAll(storedKeys);
			throw new IllegalStateException("카드 뒷면 이미지를 저장할 수 없습니다.", exception);
		}
	}

	@Override
	public byte[] load(String key) {
		try {
			return Files.readAllBytes(resolveSafely(key));
		} catch (IOException exception) {
			throw new IllegalStateException("카드 뒷면 이미지를 읽을 수 없습니다.", exception);
		}
	}

	@Override
	public void deleteAll(List<String> keys) {
		for (String key : keys) {
			try {
				Files.deleteIfExists(resolveSafely(key));
			} catch (IOException ignored) {
				// 실패한 카드 저장을 되돌리는 최선 노력 정리입니다.
			}
		}
	}

	private Path resolveSafely(String key) {
		Path resolved = root.resolve(key).normalize();
		if (!resolved.startsWith(root)) {
			throw new IllegalArgumentException("허용되지 않는 이미지 저장 키입니다.");
		}
		return resolved;
	}
}
