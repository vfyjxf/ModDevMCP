package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.ui.UiSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public final class UiCaptureRenderer {

    public byte[] render(UiSnapshot snapshot, List<UiTarget> capturedTargets, List<UiTarget> excludedTargets) {
        var width = Math.max(320, maxX(snapshot.targets()) + 24);
        var height = Math.max(240, maxY(snapshot.targets()) + 24);
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(30, 33, 40));
            graphics.fillRect(0, 0, width, height);

            graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            drawHeader(graphics, snapshot, width);
            drawTargets(graphics, snapshot.targets(), new Color(120, 180, 255), false);
            drawTargets(graphics, capturedTargets, new Color(92, 201, 110), true);
            drawTargets(graphics, excludedTargets, new Color(220, 84, 84), true);
        } finally {
            graphics.dispose();
        }
        return writePng(image);
    }

    private void drawHeader(Graphics2D graphics, UiSnapshot snapshot, int width) {
        graphics.setColor(new Color(45, 50, 61));
        graphics.fillRect(0, 0, width, 28);
        graphics.setColor(Color.WHITE);
        graphics.drawString(snapshot.driverId() + " | " + snapshot.screenClass(), 8, 18);
    }

    private void drawTargets(Graphics2D graphics, List<UiTarget> targets, Color color, boolean emphasize) {
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(emphasize ? 2.4f : 1.4f));
        for (var target : targets) {
            var bounds = target.bounds();
            graphics.drawRect(bounds.x(), bounds.y() + 28, Math.max(1, bounds.width()), Math.max(1, bounds.height()));
            var label = target.targetId() + " [" + target.role() + "]";
            graphics.drawString(label, bounds.x() + 4, bounds.y() + 42);
        }
    }

    private int maxX(List<UiTarget> targets) {
        return targets.stream().mapToInt(target -> target.bounds().x() + target.bounds().width()).max().orElse(320);
    }

    private int maxY(List<UiTarget> targets) {
        return targets.stream().mapToInt(target -> target.bounds().y() + target.bounds().height()).max().orElse(240);
    }

    private byte[] writePng(BufferedImage image) {
        try {
            var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

