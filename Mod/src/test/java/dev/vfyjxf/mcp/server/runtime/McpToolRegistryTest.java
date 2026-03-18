package dev.vfyjxf.mcp.server.runtime;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpToolRegistryTest {

    @Test
    void registryStoresNamespacedToolsAndPreventsDuplicatesPerSide() {
        var registry = new McpToolRegistry();
        var clientDefinition = new McpToolDefinition(
                "moddev.ui_snapshot",
                "UI Snapshot",
                "Reads UI",
                Map.of(),
                Map.of(),
                List.of(),
                "client",
                false,
                false,
                "public",
                "public"
        );
        var serverDefinition = new McpToolDefinition(
                "moddev.ui_snapshot",
                "UI Snapshot",
                "Reads UI",
                Map.of(),
                Map.of(),
                List.of(),
                "server",
                false,
                false,
                "public",
                "public"
        );

        registry.registerTool(clientDefinition, (context, args) -> ToolResult.success(Map.of("side", "client")));
        registry.registerTool(serverDefinition, (context, args) -> ToolResult.success(Map.of("side", "server")));

        assertEquals("client", registry.findTool("moddev.ui_snapshot", "client").orElseThrow().definition().side());
        assertEquals("server", registry.findTool("moddev.ui_snapshot", "server").orElseThrow().definition().side());
        assertThrows(IllegalArgumentException.class,
                () -> registry.registerTool(clientDefinition, (context, args) -> ToolResult.success(Map.of())));
    }
}
