package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiCaptureImage;
import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiOffscreenCaptureProvider;
import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.List;

public final class VanillaOffscreenCaptureProvider implements UiOffscreenCaptureProvider {

    @Override
    public String providerId() {
        return "vanilla-offscreen";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean matches(UiContext context, UiSnapshot snapshot) {
        return VanillaUiCaptureAvailability.hasMatchingLiveScreen(context)
                && VanillaUiCaptureAvailability.supportsOffscreenSnapshot(snapshot);
    }

    @Override
    public UiCaptureImage capture(
            UiContext context,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        return VanillaUiCaptureSupport.captureOffscreen(providerId(), context);
    }
}
