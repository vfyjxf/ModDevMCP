package dev.vfyjxf.moddev.runtime.ui;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.vfyjxf.moddev.api.runtime.UiCaptureImage;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

final class VanillaUiCaptureSupport {

    private static final long RENDER_TIMEOUT_SECONDS = 5;

    private VanillaUiCaptureSupport() {
    }

    static UiCaptureImage captureOffscreen(String providerId, UiContext context) {
        return onRenderThread(() -> doCaptureOffscreen(providerId, context));
    }

    static UiCaptureImage captureFramebuffer(String providerId, UiContext context) {
        return onRenderThread(() -> doCaptureFramebuffer(providerId, context));
    }

    private static UiCaptureImage doCaptureOffscreen(String providerId, UiContext context) {
        var minecraft = Minecraft.getInstance();
        var screen = requireMatchingScreen(minecraft, context);
        var guiWidth = Math.max(1, minecraft.getWindow().getGuiScaledWidth());
        var guiHeight = Math.max(1, minecraft.getWindow().getGuiScaledHeight());
        var guiScale = minecraft.getWindow().getGuiScale();
        var pixelWidth = Math.max(1, (int) Math.round(guiWidth * guiScale));
        var pixelHeight = Math.max(1, (int) Math.round(guiHeight * guiScale));
        var renderTarget = new TextureTarget(pixelWidth, pixelHeight, true, Minecraft.ON_OSX);
        try {
            renderTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            renderTarget.clear(Minecraft.ON_OSX);
            renderTarget.bindWrite(true);

            var savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            var modelViewStack = RenderSystem.getModelViewStack();
            var farPlane = ClientHooks.getGuiFarPlane();
            try {
                var ortho = new Matrix4f().setOrtho(
                        0.0f,
                        (float) guiWidth,
                        (float) guiHeight,
                        0.0f,
                        1000.0f,
                        farPlane
                );
                RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);
                modelViewStack.pushMatrix();
                modelViewStack.identity();
                modelViewStack.translation(0.0f, 0.0f, 10000.0f - farPlane);
                RenderSystem.applyModelViewMatrix();
                Lighting.setupFor3DItems();

                var graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
                screen.renderWithTooltip(graphics, context.mouseX(), context.mouseY(), 0.0f);
                graphics.flush();
            } finally {
                modelViewStack.popMatrix();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(savedProjection, VertexSorting.ORTHOGRAPHIC_Z);
                renderTarget.unbindWrite();
                minecraft.getMainRenderTarget().bindWrite(true);
            }

            try (var image = new NativeImage(pixelWidth, pixelHeight, false)) {
                RenderSystem.bindTexture(renderTarget.getColorTextureId());
                image.downloadTexture(0, false);
                flipVertically(image);
                return new UiCaptureImage(
                        providerId,
                        "offscreen",
                        encodePng(image),
                        pixelWidth,
                        pixelHeight,
                        metadata(screen, guiWidth, guiHeight, guiScale, "offscreen")
                );
            }
        } finally {
            renderTarget.destroyBuffers();
        }
    }

    private static UiCaptureImage doCaptureFramebuffer(String providerId, UiContext context) {
        var minecraft = Minecraft.getInstance();
        var screen = requireMatchingScreen(minecraft, context);
        var width = Math.max(1, minecraft.getWindow().getWidth());
        var height = Math.max(1, minecraft.getWindow().getHeight());
        var guiScale = minecraft.getWindow().getGuiScale();
        try (var image = new NativeImage(width, height, false)) {
            RenderSystem.bindTexture(minecraft.getMainRenderTarget().getColorTextureId());
            image.downloadTexture(0, false);
            flipVertically(image);
            return new UiCaptureImage(
                    providerId,
                    "framebuffer",
                    encodePng(image),
                    width,
                    height,
                    metadata(screen, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), guiScale, "framebuffer")
            );
        }
    }

    private static Screen requireMatchingScreen(Minecraft minecraft, UiContext context) {
        var screen = minecraft.screen;
        if (screen == null || !screen.getClass().getName().equals(context.screenClass())) {
            throw new IllegalStateException("No matching live screen for " + context.screenClass());
        }
        return screen;
    }

    private static UiCaptureImage onRenderThread(Supplier<UiCaptureImage> action) {
        if (RenderSystem.isOnRenderThread()) {
            return action.get();
        }
        var future = java.util.concurrent.CompletableFuture.supplyAsync(action, runnable -> RenderSystem.recordRenderCall(runnable::run));
        try {
            return future.get(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for render capture", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Render capture failed", exception.getCause());
        } catch (TimeoutException exception) {
            throw new IllegalStateException("Timed out waiting for render capture", exception);
        }
    }

    private static Map<String, Object> metadata(Screen screen, int guiWidth, int guiHeight, double guiScale, String mode) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("screenClass", screen.getClass().getName());
        metadata.put("guiWidth", guiWidth);
        metadata.put("guiHeight", guiHeight);
        metadata.put("guiScale", guiScale);
        metadata.put("captureMode", mode);
        return Map.copyOf(metadata);
    }

    private static byte[] encodePng(NativeImage image) {
        try {
            var path = Files.createTempFile("moddevmcp-capture-", ".png");
            try {
                image.writeToFile(path);
                return UiCapturePngNormalizer.normalizeToOpaquePng(Files.readAllBytes(path));
            } finally {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void flipVertically(NativeImage image) {
        for (int y = 0; y < image.getHeight() / 2; y++) {
            var oppositeY = image.getHeight() - 1 - y;
            for (int x = 0; x < image.getWidth(); x++) {
                var top = image.getPixelRGBA(x, y);
                var bottom = image.getPixelRGBA(x, oppositeY);
                image.setPixelRGBA(x, y, bottom);
                image.setPixelRGBA(x, oppositeY, top);
            }
        }
    }
}

