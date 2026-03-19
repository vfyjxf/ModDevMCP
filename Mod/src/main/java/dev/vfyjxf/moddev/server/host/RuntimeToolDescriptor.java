package dev.vfyjxf.moddev.server.host;

import dev.vfyjxf.moddev.server.api.McpToolDefinition;

public record RuntimeToolDescriptor(
        McpToolDefinition definition,
        String scope,
        String runtimeSide,
        boolean requiresGame,
        boolean mutating
) {
}


