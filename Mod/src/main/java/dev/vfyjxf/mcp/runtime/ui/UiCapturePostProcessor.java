package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiCaptureImage;
import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UiCapturePostProcessor {

    private UiCapturePostProcessor() {
    }

    public static UiCaptureImage process(
            UiCaptureImage source,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        var sourceImage = decode(source.pngBytes());
        var scaleX = scaleFactor(source.metadata().get("guiWidth"), sourceImage.getWidth());
        var scaleY = scaleFactor(source.metadata().get("guiHeight"), sourceImage.getHeight());
        var imageBounds = new Rectangle(0, 0, sourceImage.getWidth(), sourceImage.getHeight());
        var capturedRectangles = capturedTargets.stream()
                .map(target -> toImageRectangle(target, scaleX, scaleY, imageBounds))
                .filter(rectangle -> rectangle.width > 0 && rectangle.height > 0)
                .toList();
        var excludedRectangles = excludedTargets.stream()
                .map(target -> toImageRectangle(target, scaleX, scaleY, imageBounds))
                .filter(rectangle -> rectangle.width > 0 && rectangle.height > 0)
                .toList();

        var cropBounds = cropBounds(request.mode(), capturedRectangles, imageBounds);
        var processedImage = new BufferedImage(cropBounds.width, cropBounds.height, BufferedImage.TYPE_INT_RGB);
        var graphics = processedImage.createGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, cropBounds.width, cropBounds.height);
            graphics.drawImage(
                    sourceImage,
                    0,
                    0,
                    cropBounds.width,
                    cropBounds.height,
                    cropBounds.x,
                    cropBounds.y,
                    cropBounds.x + cropBounds.width,
                    cropBounds.y + cropBounds.height,
                    null
            );
            graphics.setColor(Color.BLACK);
            for (var excludedRectangle : excludedRectangles) {
                var translated = excludedRectangle.intersection(cropBounds);
                if (translated.isEmpty()) {
                    continue;
                }
                graphics.fillRect(
                        translated.x - cropBounds.x,
                        translated.y - cropBounds.y,
                        translated.width,
                        translated.height
                );
            }
        } finally {
            graphics.dispose();
        }

        var metadata = new LinkedHashMap<String, Object>();
        metadata.putAll(source.metadata());
        metadata.put("postProcessed", true);
        if ("crop".equalsIgnoreCase(request.mode())) {
            metadata.put("captureMode", "crop");
        }
        return new UiCaptureImage(
                source.providerId(),
                source.source(),
                encode(processedImage),
                processedImage.getWidth(),
                processedImage.getHeight(),
                Map.copyOf(metadata)
        );
    }

    private static BufferedImage decode(byte[] pngBytes) {
        try {
            var image = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (image == null) {
                throw new IOException("Unable to decode capture image");
            }
            return image;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static byte[] encode(BufferedImage image) {
        try {
            var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Rectangle cropBounds(String mode, List<Rectangle> capturedRectangles, Rectangle imageBounds) {
        if (!"crop".equalsIgnoreCase(mode) || capturedRectangles.isEmpty()) {
            return new Rectangle(imageBounds);
        }
        var union = new Rectangle(capturedRectangles.getFirst());
        for (int index = 1; index < capturedRectangles.size(); index++) {
            union = union.union(capturedRectangles.get(index));
        }
        return union.intersection(imageBounds);
    }

    private static Rectangle toImageRectangle(UiTarget target, double scaleX, double scaleY, Rectangle imageBounds) {
        var bounds = target.bounds();
        var left = clamp((int) Math.floor(bounds.x() * scaleX), 0, imageBounds.width);
        var top = clamp((int) Math.floor(bounds.y() * scaleY), 0, imageBounds.height);
        var right = clamp((int) Math.ceil((bounds.x() + bounds.width()) * scaleX), left, imageBounds.width);
        var bottom = clamp((int) Math.ceil((bounds.y() + bounds.height()) * scaleY), top, imageBounds.height);
        return new Rectangle(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    private static double scaleFactor(Object guiSizeValue, int imageSize) {
        if (guiSizeValue instanceof Number number && number.doubleValue() > 0.0d) {
            return imageSize / number.doubleValue();
        }
        return 1.0d;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
