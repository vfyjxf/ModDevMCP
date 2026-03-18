package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;

public record RuntimeToolDescriptor(
        McpToolDefinition definition,
        String scope,
        String runtimeSide,
        boolean requiresGame,
        boolean mutating
) {
}

