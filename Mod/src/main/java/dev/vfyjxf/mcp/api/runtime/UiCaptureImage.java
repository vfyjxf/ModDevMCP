package dev.vfyjxf.mcp.api.runtime;

import java.util.Map;

public record UiCaptureImage(
        String providerId,
        String source,
        byte[] pngBytes,
        int width,
        int height,
        Map<String, Object> metadata
) {
    public UiCaptureImage {
        pngBytes = pngBytes == null ? new byte[0] : pngBytes.clone();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
