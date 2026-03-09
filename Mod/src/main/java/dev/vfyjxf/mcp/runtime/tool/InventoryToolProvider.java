package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.List;
import java.util.Map;

public final class InventoryToolProvider implements McpToolProvider {

    private final RuntimeRegistries registries;

    public InventoryToolProvider(RuntimeRegistries registries) {
        this.registries = registries;
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(
                new McpToolDefinition("moddev.inventory_snapshot", "moddev.inventory_snapshot", "Built-in inventory snapshot tool", Map.of(), Map.of(), List.of("inventory"), "either", false, false, "public", "public"),
                (context, arguments) -> ToolResult.success(Map.of("tool", "moddev.inventory_snapshot", "registeredInventoryDrivers", registries.inventoryDrivers().size()))
        );
        registry.registerTool(
                new McpToolDefinition("moddev.inventory_action", "moddev.inventory_action", "Built-in inventory action tool", Map.of(), Map.of(), List.of("inventory"), "either", false, false, "public", "public"),
                (context, arguments) -> ToolResult.success(Map.of("tool", "moddev.inventory_action", "registeredInventoryDrivers", registries.inventoryDrivers().size()))
        );
    }
}
