package dev.vfyjxf.mcp.server.api;

import java.util.Map;

public record ToolCallContext(
        String side,
        Map<String, Object> metadata
) {
    public static ToolCallContext empty() {
        return new ToolCallContext("either", Map.of());
    }
}
