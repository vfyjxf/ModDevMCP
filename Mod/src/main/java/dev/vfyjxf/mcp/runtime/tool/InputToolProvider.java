package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.List;
import java.util.Map;

public final class InputToolProvider implements McpToolProvider {

    private final RuntimeRegistries registries;

    public InputToolProvider(RuntimeRegistries registries) {
        this.registries = registries;
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(
                new McpToolDefinition("moddev.input_action", "moddev.input_action", "Built-in input tool", Map.of(), Map.of(), List.of("input"), "client", false, false, "public", "public"),
                (context, arguments) -> ToolResult.success(Map.of("tool", "moddev.input_action", "registeredInputControllers", registries.inputControllers().size()))
        );
    }
}
