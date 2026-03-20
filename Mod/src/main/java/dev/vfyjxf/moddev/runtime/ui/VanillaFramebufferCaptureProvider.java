package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.UiCaptureImage;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiFramebufferCaptureProvider;
import dev.vfyjxf.moddev.api.ui.CaptureRequest;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;

import java.util.List;

public final class VanillaFramebufferCaptureProvider implements UiFramebufferCaptureProvider {

    @Override
    public String providerId() {
        return "vanilla-framebuffer";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean matches(UiContext context, UiSnapshot snapshot) {
        return VanillaUiCaptureAvailability.supportsFramebufferCapture(context);
    }

    @Override
    public UiCaptureImage capture(
            UiContext context,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        return VanillaUiCaptureSupport.captureFramebuffer(providerId(), context);
    }
}


