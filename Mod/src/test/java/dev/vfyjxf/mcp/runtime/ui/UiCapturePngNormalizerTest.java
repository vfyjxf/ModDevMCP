package dev.vfyjxf.mcp.runtime.ui;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCapturePngNormalizerTest {

    @Test
    void normalizeToOpaquePngPreservesRgbButRemovesAlpha() throws IOException {
        var source = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, new Color(61, 29, 16, 64).getRGB());
        source.setRGB(1, 0, new Color(75, 37, 21, 255).getRGB());

        var output = new ByteArrayOutputStream();
        ImageIO.write(source, "png", output);

        var normalizedBytes = UiCapturePngNormalizer.normalizeToOpaquePng(output.toByteArray());
        var normalized = ImageIO.read(new ByteArrayInputStream(normalizedBytes));

        var left = new Color(normalized.getRGB(0, 0), true);
        var right = new Color(normalized.getRGB(1, 0), true);

        assertEquals(255, left.getAlpha());
        assertEquals(61, left.getRed());
        assertEquals(29, left.getGreen());
        assertEquals(16, left.getBlue());

        assertEquals(255, right.getAlpha());
        assertEquals(75, right.getRed());
        assertEquals(37, right.getGreen());
        assertEquals(21, right.getBlue());
    }
}
