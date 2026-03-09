package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.List;
import java.util.Map;

public final class EventToolProvider implements McpToolProvider {

    private final RuntimeRegistries registries;

    public EventToolProvider(RuntimeRegistries registries) {
        this.registries = registries;
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(
                new McpToolDefinition("moddev.event_poll", "moddev.event_poll", "Built-in event polling tool", Map.of(), Map.of(), List.of("event"), "either", false, false, "public", "public"),
                (context, arguments) -> ToolResult.success(Map.of("tool", "moddev.event_poll", "registeredToolProviders", registries.toolProviders().size()))
        );
        registry.registerTool(
                new McpToolDefinition("moddev.event_subscribe", "moddev.event_subscribe", "Built-in event subscribe tool", Map.of(), Map.of(), List.of("event"), "either", false, false, "public", "public"),
                (context, arguments) -> ToolResult.success(Map.of("tool", "moddev.event_subscribe", "registeredToolProviders", registries.toolProviders().size()))
        );
    }
}
