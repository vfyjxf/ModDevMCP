package dev.vfyjxf.mcp.runtime.ui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

final class UiCapturePngNormalizer {

    private UiCapturePngNormalizer() {
    }

    static byte[] normalizeToOpaquePng(byte[] pngBytes) {
        try {
            var source = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (source == null) {
                throw new IOException("Unable to decode PNG bytes");
            }
            var normalized = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    normalized.setRGB(x, y, source.getRGB(x, y));
                }
            }
            var output = new ByteArrayOutputStream();
            ImageIO.write(normalized, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
