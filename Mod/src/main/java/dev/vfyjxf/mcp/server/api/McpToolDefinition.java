package dev.vfyjxf.mcp.server.api;

import java.util.List;
import java.util.Map;

public record McpToolDefinition(
        String name,
        String title,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        List<String> tags,
        String side,
        boolean requiresWorld,
        boolean requiresPlayer,
        String availability,
        String exposurePolicy
) {
}
