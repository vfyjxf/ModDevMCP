package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinProviderRegistrationTest {

    @Test
    void modStartupRegistersBuiltinProvidersIntoServer() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        mod.registerBuiltinProviders();

        assertTrue(server.registry().findTool("moddev.ui_snapshot").isPresent());
        assertTrue(server.registry().findTool("moddev.inventory_snapshot").isPresent());
        assertTrue(server.registry().findTool("moddev.compile").isPresent());
        assertTrue(server.registry().findTool("moddev.hotswap").isPresent());
    }

    @Test
    void modStartupRegistersBuiltinRealCaptureProvidersIntoRuntime() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        assertEquals(1, mod.registries().uiOffscreenCaptureProviders().size());
        assertEquals(1, mod.registries().uiFramebufferCaptureProviders().size());
    }
}
