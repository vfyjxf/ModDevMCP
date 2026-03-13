package dev.vfyjxf.mcp.api.runtime;

public record ClientScreenMetrics(
        String screenClass,
        int guiWidth,
        int guiHeight,
        int framebufferWidth,
        int framebufferHeight
) {
}
