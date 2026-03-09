package dev.vfyjxf.mcp.server.runtime;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class McpToolRegistryTest {

    @Test
    void registryStoresNamespacedToolsAndPreventsDuplicates() {
        var registry = new McpToolRegistry();
        var definition = new McpToolDefinition(
                "moddev.ui_snapshot",
                "UI Snapshot",
                "Reads UI",
                Map.of(),
                Map.of(),
                List.of(),
                "either",
                false,
                false,
                "public",
                "public"
        );

        registry.registerTool(definition, (context, args) -> ToolResult.success(Map.of("ok", true)));

        assertThrows(IllegalArgumentException.class,
                () -> registry.registerTool(definition, (context, args) -> ToolResult.success(Map.of())));
    }
}
