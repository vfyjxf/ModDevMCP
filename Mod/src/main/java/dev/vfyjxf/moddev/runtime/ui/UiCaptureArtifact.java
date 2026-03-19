package dev.vfyjxf.moddev.runtime.ui;

import java.util.Map;

public record UiCaptureArtifact(
        String imageRef,
        String path,
        String resourceUri,
        String mimeType,
        Map<String, Object> metadata,
        byte[] content
) {
    public UiCaptureArtifact {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        content = content == null ? new byte[0] : content.clone();
    }
}

