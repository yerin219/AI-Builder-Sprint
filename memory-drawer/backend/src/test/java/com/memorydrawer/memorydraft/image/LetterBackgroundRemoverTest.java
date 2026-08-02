package com.memorydrawer.memorydraft.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class LetterBackgroundRemoverTest {

	private final LetterBackgroundRemover remover = new LetterBackgroundRemover();

	@Test
	void removesBrightUniformBackgroundAndKeepsDarkWriting() throws Exception {
		BufferedImage source = simpleLetterImage();

		byte[] result = remover.remove(png(source)).orElseThrow();
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));

		assertThat(decoded.getColorModel().hasAlpha()).isTrue();
		assertThat(alpha(decoded.getRGB(0, 0))).isZero();
		assertThat(alpha(decoded.getRGB(60, 38))).isGreaterThan(240);
	}

	@Test
	void removesBackgroundFromDecodableJpeg() throws Exception {
		byte[] result = remover.remove(imageBytes(simpleLetterImage(), "jpg")).orElseThrow();
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));

		assertThat(decoded.getColorModel().hasAlpha()).isTrue();
		assertThat(alpha(decoded.getRGB(0, 0))).isZero();
		assertThat(alpha(decoded.getRGB(60, 38))).isGreaterThan(200);
	}

	@Test
	void acceptsNormalExifOrientationAndRejectsRotatedOrientation() throws Exception {
		byte[] jpeg = imageBytes(simpleLetterImage(), "jpg");

		assertThat(remover.remove(withExifOrientation(jpeg, 1))).isPresent();
		assertThat(remover.remove(withExifOrientation(jpeg, 6))).isEmpty();
	}

	@Test
	void rejectsMalformedExifOrientationMetadata() throws Exception {
		byte[] jpeg = imageBytes(simpleLetterImage(), "jpg");
		ByteArrayOutputStream malformed = new ByteArrayOutputStream();
		malformed.write(jpeg, 0, 2);
		malformed.writeBytes(new byte[] {
			(byte)0xff, (byte)0xe1, 0, 8, 'E', 'x', 'i', 'f', 0, 0
		});
		malformed.write(jpeg, 2, jpeg.length - 2);

		assertThat(remover.remove(malformed.toByteArray())).isEmpty();
	}

	@Test
	void rejectsBlankPageWithoutEnoughForeground() throws Exception {
		BufferedImage source = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = source.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
		} finally {
			graphics.dispose();
		}

		assertThat(remover.remove(png(source))).isEmpty();
	}

	@Test
	void rejectsComplexBorderInsteadOfProducingLowQualityCutout() throws Exception {
		BufferedImage source = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				source.setRGB(x, y, ((x / 5 + y / 5) % 2 == 0 ? Color.WHITE : Color.BLACK).getRGB());
			}
		}

		assertThat(remover.remove(png(source))).isEmpty();
	}

	@Test
	void returnsEmptyForUnsupportedOrUndecodableImage() {
		byte[] webpHeader = new byte[] {
			'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'
		};

		assertThat(remover.remove(webpHeader)).isEmpty();
		assertThat(remover.remove(new byte[] {1, 2, 3})).isEmpty();
	}

	private byte[] png(BufferedImage image) throws Exception {
		return imageBytes(image, "png");
	}

	private byte[] imageBytes(BufferedImage image, String format) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, format, output);
		return output.toByteArray();
	}

	private BufferedImage simpleLetterImage() {
		BufferedImage source = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = source.createGraphics();
		try {
			graphics.setColor(new Color(245, 242, 235));
			graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
			graphics.setColor(new Color(35, 30, 28));
			graphics.fillRect(20, 34, 80, 8);
		} finally {
			graphics.dispose();
		}
		return source;
	}

	private byte[] withExifOrientation(byte[] jpeg, int orientation) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		output.write(jpeg, 0, 2);
		output.writeBytes(new byte[] {
			(byte)0xff, (byte)0xe1, 0, 34,
			'E', 'x', 'i', 'f', 0, 0,
			'I', 'I', 42, 0,
			8, 0, 0, 0,
			1, 0,
			18, 1, 3, 0,
			1, 0, 0, 0,
			(byte)orientation, 0, 0, 0,
			0, 0, 0, 0
		});
		output.write(jpeg, 2, jpeg.length - 2);
		return output.toByteArray();
	}

	private int alpha(int pixel) {
		return pixel >>> 24 & 0xff;
	}
}
