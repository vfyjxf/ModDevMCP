package dev.vfyjxf.moddev.server.api;

import java.util.Map;

public record McpResource(
        String uri,
        String mimeType,
        String displayName,
        Map<String, Object> metadata,
        byte[] content
) {
    public McpResource {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        content = content == null ? new byte[0] : content.clone();
    }
}

