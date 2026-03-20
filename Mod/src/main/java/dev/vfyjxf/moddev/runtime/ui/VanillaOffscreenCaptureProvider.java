package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.UiCaptureImage;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiOffscreenCaptureProvider;
import dev.vfyjxf.moddev.api.ui.CaptureRequest;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;

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

