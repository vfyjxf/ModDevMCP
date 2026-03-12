package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinProviderRegistrationTest {

    @Test
    void modStartupRegistersBuiltinProvidersIntoServer() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        mod.registerBuiltinProviders();

        assertTrue(server.registry().findTool("moddev.ui_snapshot").isPresent());
        assertTrue(server.registry().findTool("moddev.ui_get_live_screen").isPresent());
        assertTrue(server.registry().findTool("moddev.inventory_snapshot").isPresent());
    }

    @Test
    void modStartupRegistersBuiltinRealCaptureProvidersIntoRuntime() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        assertEquals(1, mod.registries().uiOffscreenCaptureProviders().size());
        assertEquals(1, mod.registries().uiFramebufferCaptureProviders().size());
    }

    @Test
    void modStartupRegistersCustomToolProvidersIntoServerWithoutDuplicates() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.api().registerToolProvider(registry -> registry.registerTool(
                new McpToolDefinition("demo.extra", "Extra", "Extra tool", Map.of(), Map.of(), List.of(), "either", false, false, "public", "public"),
                (context, arguments) -> ToolResult.success(arguments)
        ));

        mod.registerBuiltinProviders();
        mod.registerBuiltinProviders();

        assertTrue(server.registry().findTool("demo.extra").isPresent());
        assertEquals(1, server.registry().listTools().stream()
                .filter(tool -> tool.definition().name().equals("demo.extra"))
                .count());
    }
}
