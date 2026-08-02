package com.memorydrawer.memorydraft.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

@Component
public class LetterBackgroundRemover {

	private static final int MAX_DIMENSION = 8_192;
	private static final long MAX_PIXEL_COUNT = 16_000_000L;
	private static final int MAX_OUTPUT_BYTES = 10 * 1_024 * 1_024;
	private static final int MAX_BORDER_SAMPLES = 4_096;
	private static final double MIN_UNIFORM_BORDER_FRACTION = 0.85;
	private static final double MIN_BACKGROUND_LUMINANCE = 170.0;
	private static final int MAX_BACKGROUND_CHROMA = 70;
	private static final double MIN_TRANSPARENT_FRACTION = 0.45;
	private static final double MAX_VISIBLE_FRACTION = 0.45;
	private static final double MIN_TRANSPARENT_BORDER_FRACTION = 0.80;
	private static final double CLEAR_DISTANCE = 42.0;
	private static final double OPAQUE_DISTANCE = 90.0;

	public Optional<byte[]> remove(byte[] source) {
		if (source == null || source.length == 0) {
			return Optional.empty();
		}
		OrientationStatus orientation = jpegOrientation(source);
		if (orientation == OrientationStatus.UNSUPPORTED
			|| orientation == OrientationStatus.INVALID) {
			return Optional.empty();
		}

		try {
			BufferedImage image = decode(source);
			if (image == null) {
				return Optional.empty();
			}
			BackgroundColor background = estimateBackground(image);
			if (background == null) {
				return Optional.empty();
			}
			return removeAndValidate(image, background);
		} catch (IOException | RuntimeException exception) {
			return Optional.empty();
		}
	}

	private OrientationStatus jpegOrientation(byte[] source) {
		if (source.length < 2 || unsigned(source[0]) != 0xff || unsigned(source[1]) != 0xd8) {
			return OrientationStatus.ABSENT;
		}

		int offset = 2;
		while (offset < source.length) {
			if (unsigned(source[offset]) != 0xff) {
				return OrientationStatus.INVALID;
			}
			while (offset < source.length && unsigned(source[offset]) == 0xff) {
				offset++;
			}
			if (offset >= source.length) {
				return OrientationStatus.INVALID;
			}

			int marker = unsigned(source[offset++]);
			if (marker == 0xd9 || marker == 0xda) {
				return OrientationStatus.ABSENT;
			}
			if (marker == 0x01 || marker >= 0xd0 && marker <= 0xd7) {
				continue;
			}
			if (offset + 2 > source.length) {
				return OrientationStatus.INVALID;
			}

			int segmentLength = unsignedShortBigEndian(source, offset);
			if (segmentLength < 2 || offset > source.length - segmentLength) {
				return OrientationStatus.INVALID;
			}
			int payloadOffset = offset + 2;
			int payloadLength = segmentLength - 2;
			if (marker == 0xe1 && startsWithExif(source, payloadOffset, payloadLength)) {
				return parseExifOrientation(source, payloadOffset, payloadLength);
			}
			offset += segmentLength;
		}
		return OrientationStatus.INVALID;
	}

	private boolean startsWithExif(byte[] source, int offset, int length) {
		return length >= 6
			&& source[offset] == 'E'
			&& source[offset + 1] == 'x'
			&& source[offset + 2] == 'i'
			&& source[offset + 3] == 'f'
			&& source[offset + 4] == 0
			&& source[offset + 5] == 0;
	}

	private OrientationStatus parseExifOrientation(byte[] source, int offset, int length) {
		int tiffOffset = offset + 6;
		int tiffLength = length - 6;
		if (tiffLength < 8) {
			return OrientationStatus.INVALID;
		}

		boolean littleEndian;
		if (source[tiffOffset] == 'I' && source[tiffOffset + 1] == 'I') {
			littleEndian = true;
		} else if (source[tiffOffset] == 'M' && source[tiffOffset + 1] == 'M') {
			littleEndian = false;
		} else {
			return OrientationStatus.INVALID;
		}
		if (unsignedShort(source, tiffOffset + 2, littleEndian) != 42) {
			return OrientationStatus.INVALID;
		}

		long relativeIfdOffset = unsignedInt(source, tiffOffset + 4, littleEndian);
		if (relativeIfdOffset > Integer.MAX_VALUE) {
			return OrientationStatus.INVALID;
		}
		int ifdOffset = tiffOffset + (int)relativeIfdOffset;
		int tiffEnd = tiffOffset + tiffLength;
		if (ifdOffset < tiffOffset || ifdOffset > tiffEnd - 2) {
			return OrientationStatus.INVALID;
		}

		int entryCount = unsignedShort(source, ifdOffset, littleEndian);
		long entriesEnd = (long)ifdOffset + 2L + (long)entryCount * 12L;
		if (entriesEnd > tiffEnd) {
			return OrientationStatus.INVALID;
		}
		for (int index = 0; index < entryCount; index++) {
			int entryOffset = ifdOffset + 2 + index * 12;
			if (unsignedShort(source, entryOffset, littleEndian) != 0x0112) {
				continue;
			}
			int type = unsignedShort(source, entryOffset + 2, littleEndian);
			long count = unsignedInt(source, entryOffset + 4, littleEndian);
			if (type != 3 || count != 1) {
				return OrientationStatus.INVALID;
			}
			int orientation = unsignedShort(source, entryOffset + 8, littleEndian);
			if (orientation == 1) {
				return OrientationStatus.NORMAL;
			}
			return orientation >= 2 && orientation <= 8
				? OrientationStatus.UNSUPPORTED
				: OrientationStatus.INVALID;
		}
		return OrientationStatus.ABSENT;
	}

	private int unsignedShortBigEndian(byte[] source, int offset) {
		return unsigned(source[offset]) << 8 | unsigned(source[offset + 1]);
	}

	private int unsignedShort(byte[] source, int offset, boolean littleEndian) {
		return littleEndian
			? unsigned(source[offset]) | unsigned(source[offset + 1]) << 8
			: unsigned(source[offset]) << 8 | unsigned(source[offset + 1]);
	}

	private long unsignedInt(byte[] source, int offset, boolean littleEndian) {
		if (littleEndian) {
			return (long)unsigned(source[offset])
				| (long)unsigned(source[offset + 1]) << 8
				| (long)unsigned(source[offset + 2]) << 16
				| (long)unsigned(source[offset + 3]) << 24;
		}
		return (long)unsigned(source[offset]) << 24
			| (long)unsigned(source[offset + 1]) << 16
			| (long)unsigned(source[offset + 2]) << 8
			| unsigned(source[offset + 3]);
	}

	private BufferedImage decode(byte[] source) throws IOException {
		try (ByteArrayInputStream bytes = new ByteArrayInputStream(source);
			 ImageInputStream input = ImageIO.createImageInputStream(bytes)) {
			if (input == null) {
				return null;
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				return null;
			}

			ImageReader reader = readers.next();
			try {
				reader.setInput(input, false, true);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (width < 2 || height < 2
					|| width > MAX_DIMENSION || height > MAX_DIMENSION
					|| (long)width * height > MAX_PIXEL_COUNT) {
					return null;
				}
				return reader.read(0);
			} finally {
				reader.dispose();
			}
		}
	}

	private BackgroundColor estimateBackground(BufferedImage image) {
		List<Integer> samples = borderSamples(image);
		if (samples.size() < 16) {
			return null;
		}

		int[] reds = new int[samples.size()];
		int[] greens = new int[samples.size()];
		int[] blues = new int[samples.size()];
		for (int index = 0; index < samples.size(); index++) {
			int pixel = samples.get(index);
			reds[index] = red(pixel);
			greens[index] = green(pixel);
			blues[index] = blue(pixel);
		}
		Arrays.sort(reds);
		Arrays.sort(greens);
		Arrays.sort(blues);
		BackgroundColor background = new BackgroundColor(
			reds[reds.length / 2],
			greens[greens.length / 2],
			blues[blues.length / 2]
		);

		if (background.luminance() < MIN_BACKGROUND_LUMINANCE
			|| background.chroma() > MAX_BACKGROUND_CHROMA) {
			return null;
		}

		long similar = samples.stream()
			.filter(pixel -> colorDistance(pixel, background) <= CLEAR_DISTANCE)
			.count();
		return (double)similar / samples.size() >= MIN_UNIFORM_BORDER_FRACTION
			? background
			: null;
	}

	private List<Integer> borderSamples(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int perimeter = 2 * width + 2 * Math.max(0, height - 2);
		int step = Math.max(1, perimeter / MAX_BORDER_SAMPLES);
		List<Integer> samples = new ArrayList<>(Math.min(perimeter, MAX_BORDER_SAMPLES + 4));

		for (int x = 0; x < width; x += step) {
			addOpaque(samples, image.getRGB(x, 0));
			addOpaque(samples, image.getRGB(x, height - 1));
		}
		for (int y = 1; y < height - 1; y += step) {
			addOpaque(samples, image.getRGB(0, y));
			addOpaque(samples, image.getRGB(width - 1, y));
		}
		return samples;
	}

	private void addOpaque(List<Integer> samples, int pixel) {
		if (alpha(pixel) >= 224) {
			samples.add(pixel);
		}
	}

	private Optional<byte[]> removeAndValidate(
		BufferedImage source,
		BackgroundColor background
	) throws IOException {
		int width = source.getWidth();
		int height = source.getHeight();
		long pixelCount = (long)width * height;
		long transparentPixels = 0;
		long visiblePixels = 0;
		long transparentBorderPixels = 0;
		long borderPixels = 0;
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int sourcePixel = source.getRGB(x, y);
				int outputAlpha = outputAlpha(sourcePixel, background);
				int outputPixel = outputAlpha << 24 | sourcePixel & 0x00ff_ffff;
				result.setRGB(x, y, outputPixel);

				if (outputAlpha <= 16) {
					transparentPixels++;
				}
				if (outputAlpha >= 96) {
					visiblePixels++;
				}
				if (isBorder(x, y, width, height)) {
					borderPixels++;
					if (outputAlpha <= 16) {
						transparentBorderPixels++;
					}
				}
			}
		}

		long minimumVisiblePixels = Math.max(24L, pixelCount / 2_000L);
		double transparentFraction = (double)transparentPixels / pixelCount;
		double visibleFraction = (double)visiblePixels / pixelCount;
		double transparentBorderFraction = (double)transparentBorderPixels / borderPixels;
		if (transparentFraction < MIN_TRANSPARENT_FRACTION
			|| visiblePixels < minimumVisiblePixels
			|| visibleFraction > MAX_VISIBLE_FRACTION
			|| transparentBorderFraction < MIN_TRANSPARENT_BORDER_FRACTION) {
			return Optional.empty();
		}

		LimitedOutputStream output = new LimitedOutputStream(MAX_OUTPUT_BYTES);
		return ImageIO.write(result, "png", output)
			? Optional.of(output.toByteArray())
			: Optional.empty();
	}

	private int outputAlpha(int pixel, BackgroundColor background) {
		int sourceAlpha = alpha(pixel);
		if (sourceAlpha == 0) {
			return 0;
		}
		double distance = colorDistance(pixel, background);
		if (distance <= CLEAR_DISTANCE) {
			return 0;
		}
		if (distance >= OPAQUE_DISTANCE) {
			return sourceAlpha;
		}
		double ratio = (distance - CLEAR_DISTANCE) / (OPAQUE_DISTANCE - CLEAR_DISTANCE);
		return (int)Math.round(sourceAlpha * ratio);
	}

	private boolean isBorder(int x, int y, int width, int height) {
		return x == 0 || y == 0 || x == width - 1 || y == height - 1;
	}

	private static double colorDistance(int pixel, BackgroundColor background) {
		int redDifference = red(pixel) - background.red();
		int greenDifference = green(pixel) - background.green();
		int blueDifference = blue(pixel) - background.blue();
		return Math.sqrt(
			redDifference * redDifference
				+ greenDifference * greenDifference
				+ blueDifference * blueDifference
		);
	}

	private static int alpha(int pixel) {
		return pixel >>> 24 & 0xff;
	}

	private static int red(int pixel) {
		return pixel >>> 16 & 0xff;
	}

	private static int green(int pixel) {
		return pixel >>> 8 & 0xff;
	}

	private static int blue(int pixel) {
		return pixel & 0xff;
	}

	private static int unsigned(byte value) {
		return value & 0xff;
	}

	private enum OrientationStatus {
		ABSENT,
		NORMAL,
		UNSUPPORTED,
		INVALID
	}

	private record BackgroundColor(int red, int green, int blue) {

		double luminance() {
			return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
		}

		int chroma() {
			return Math.max(red, Math.max(green, blue))
				- Math.min(red, Math.min(green, blue));
		}
	}

	private static final class LimitedOutputStream extends OutputStream {

		private final int limit;
		private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

		private LimitedOutputStream(int limit) {
			this.limit = limit;
		}

		@Override
		public void write(int value) throws IOException {
			ensureCapacity(1);
			delegate.write(value);
		}

		@Override
		public void write(byte[] bytes, int offset, int length) throws IOException {
			ensureCapacity(length);
			delegate.write(bytes, offset, length);
		}

		private void ensureCapacity(int additionalBytes) throws IOException {
			if (additionalBytes < 0 || delegate.size() > limit - additionalBytes) {
				throw new IOException("배경 제거 결과가 허용 크기를 초과했습니다.");
			}
		}

		private byte[] toByteArray() {
			return delegate.toByteArray();
		}
	}
}
