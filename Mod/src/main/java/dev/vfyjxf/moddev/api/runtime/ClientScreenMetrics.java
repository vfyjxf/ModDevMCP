package dev.vfyjxf.moddev.api.runtime;

/**
 * Snapshot of the active client screen identity and dimensions.
 */
public record ClientScreenMetrics(
        String screenClass,
        int guiWidth,
        int guiHeight,
        int framebufferWidth,
        int framebufferHeight
) {
}

