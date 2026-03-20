package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiCaptureImage;
import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiFramebufferCaptureProvider;
import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;

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
        return VanillaUiCaptureAvailability.hasLiveFramebuffer(context);
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
