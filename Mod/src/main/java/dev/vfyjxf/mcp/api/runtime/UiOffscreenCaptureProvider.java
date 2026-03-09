package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.List;

public interface UiOffscreenCaptureProvider {

    String providerId();

    int priority();

    boolean matches(UiContext context, UiSnapshot snapshot);

    UiCaptureImage capture(
            UiContext context,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    );
}
