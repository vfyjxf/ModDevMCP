package dev.vfyjxf.moddev.server.api;

import java.util.Map;

@FunctionalInterface
public interface McpToolHandler {

    ToolResult handle(ToolCallContext context, Map<String, Object> arguments);
}

