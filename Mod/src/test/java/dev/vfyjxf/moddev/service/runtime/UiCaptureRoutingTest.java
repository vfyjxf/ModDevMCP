package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.api.model.OperationResult;
import dev.vfyjxf.moddev.api.runtime.DriverDescriptor;
import dev.vfyjxf.moddev.api.runtime.UiCaptureImage;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiDriver;
import dev.vfyjxf.moddev.api.runtime.UiFramebufferCaptureProvider;
import dev.vfyjxf.moddev.api.runtime.UiOffscreenCaptureProvider;
import dev.vfyjxf.moddev.api.ui.Bounds;
import dev.vfyjxf.moddev.api.ui.CaptureRequest;
import dev.vfyjxf.moddev.api.ui.SnapshotOptions;
import dev.vfyjxf.moddev.api.ui.TargetSelector;
import dev.vfyjxf.moddev.api.ui.UiActionRequest;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import dev.vfyjxf.moddev.api.ui.UiTargetState;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.service.request.OperationRequest;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCaptureRoutingTest {

    @Test
    void autoCapturePrefersFramebufferProviderOverOffscreen() throws Exception {
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new FakeUiDriver());
        var offscreen = new RecordingOffscreenProvider();
        var framebuffer = new RecordingFramebufferProvider();
        registries.uiOffscreenCaptureProviders().register(offscreen);
        registries.uiFramebufferCaptureProviders().register(framebuffer);

        var bindings = new RuntimeOperationBindings(registries, () -> new RuntimeOperationBindings.StatusSnapshot(
                true,
                true,
                List.of("client"),
                "moddev-usage",
                Path.of("build/export"),
                null
        ));

        var output = bindings.execute(
                new OperationRequest(
                        "req-capture",
                        "ui.capture",
                        "client",
                        Map.of(
                                "screenClass", "dev.vfyjxf.testmod.client.TestModDebugScreen",
                                "source", "auto",
                                "mode", "full"
                        )
                ),
                "client"
        );

        assertEquals("framebuffer", output.get("source"));
        assertEquals(1, framebuffer.invocationCount.get());
        assertEquals(0, offscreen.invocationCount.get());
    }

    private static final class FakeUiDriver implements UiDriver {

        @Override
        public DriverDescriptor descriptor() {
            return new DriverDescriptor("fake-ui", "minecraft", 500, Set.of("snapshot", "capture"));
        }

        @Override
        public boolean matches(UiContext context) {
            return "dev.vfyjxf.testmod.client.TestModDebugScreen".equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    "screen",
                    context.screenClass(),
                    descriptor().id(),
                    List.of(new UiTarget(
                            "screen-root",
                            descriptor().id(),
                            context.screenClass(),
                            context.modId(),
                            "screen",
                            context.screenClass(),
                            new Bounds(0, 0, 320, 240),
                            UiTargetState.defaultState(),
                            List.of("capture"),
                            Map.of()
                    )),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, TargetSelector selector) {
            return snapshot(context, SnapshotOptions.DEFAULT).targets();
        }

        @Override
        public OperationResult<Map<String, Object>> action(UiContext context, UiActionRequest request) {
            return OperationResult.rejected("unsupported");
        }
    }

    private static final class RecordingOffscreenProvider implements UiOffscreenCaptureProvider {
        private final AtomicInteger invocationCount = new AtomicInteger();

        @Override
        public String providerId() {
            return "fake-offscreen";
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public boolean matches(UiContext context, UiSnapshot snapshot) {
            return true;
        }

        @Override
        public UiCaptureImage capture(UiContext context, UiSnapshot snapshot, CaptureRequest request, List<UiTarget> capturedTargets, List<UiTarget> excludedTargets) {
            invocationCount.incrementAndGet();
            return new UiCaptureImage(providerId(), "offscreen", pngBytes(0x00FF00), 2, 2, Map.of());
        }
    }

    private static final class RecordingFramebufferProvider implements UiFramebufferCaptureProvider {
        private final AtomicInteger invocationCount = new AtomicInteger();

        @Override
        public String providerId() {
            return "fake-framebuffer";
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public boolean matches(UiContext context, UiSnapshot snapshot) {
            return true;
        }

        @Override
        public UiCaptureImage capture(UiContext context, UiSnapshot snapshot, CaptureRequest request, List<UiTarget> capturedTargets, List<UiTarget> excludedTargets) {
            invocationCount.incrementAndGet();
            return new UiCaptureImage(providerId(), "framebuffer", pngBytes(0xFF0000), 2, 2, Map.of());
        }
    }

    private static byte[] pngBytes(int rgb) {
        try {
            var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 2; y++) {
                for (int x = 0; x < 2; x++) {
                    image.setRGB(x, y, 0xFF000000 | rgb);
                }
            }
            var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (java.io.IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
