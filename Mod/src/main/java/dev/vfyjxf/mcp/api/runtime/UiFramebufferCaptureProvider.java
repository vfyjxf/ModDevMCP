package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.List;

/**
 * Capture provider that reads imagery from the active framebuffer.
 */
public interface UiFramebufferCaptureProvider {

    /**
     * Returns the stable provider identifier.
     */
    String providerId();

    /**
     * Returns the selection priority for this provider.
     */
    int priority();

    /**
     * Returns {@code true} when this provider can capture the supplied context and snapshot.
     */
    boolean matches(UiContext context, UiSnapshot snapshot);

    /**
     * Captures an image for the supplied request and resolved targets.
     */
    UiCaptureImage capture(
            UiContext context,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    );
}
