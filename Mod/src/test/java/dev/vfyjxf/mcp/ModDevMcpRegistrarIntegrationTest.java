package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.api.event.RegisterClientMcpToolsEvent;
import dev.vfyjxf.mcp.api.event.RegisterCommonMcpToolsEvent;
import dev.vfyjxf.mcp.api.event.RegisterServerMcpToolsEvent;
import dev.vfyjxf.mcp.api.registrar.ClientMcpToolRegistrar;
import dev.vfyjxf.mcp.api.registrar.CommonMcpToolRegistrar;
import dev.vfyjxf.mcp.api.registrar.ServerMcpToolRegistrar;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDevMcpRegistrarIntegrationTest {

    @Test
    void commonRegistrarsRunDuringCommonServerPreparation() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(
                server,
                new RuntimeRegistries(),
                () -> List.of(event -> event.register(tool("demo.common"))),
                List::of,
                List::of
        );

        mod.prepareCommonServer();

        assertTrue(server.registry().findTool("demo.common").isPresent());
        assertTrue(server.registry().findTool("demo.client").isEmpty());
        assertTrue(server.registry().findTool("demo.server").isEmpty());
    }

    @Test
    void clientRegistrarsRunOnlyDuringClientPreparation() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(
                server,
                new RuntimeRegistries(),
                () -> List.of(event -> event.register(tool("demo.common"))),
                () -> List.of(event -> event.register(tool("demo.client"))),
                () -> List.of(event -> event.register(tool("demo.server")))
        );

        mod.prepareClientServer();

        assertTrue(server.registry().findTool("demo.common").isPresent());
        assertTrue(server.registry().findTool("demo.client").isPresent());
        assertTrue(server.registry().findTool("demo.server").isEmpty());
    }

    @Test
    void serverRegistrarsRunOnlyDuringServerPreparation() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(
                server,
                new RuntimeRegistries(),
                () -> List.of(event -> event.register(tool("demo.common"))),
                () -> List.of(event -> event.register(tool("demo.client"))),
                () -> List.of(event -> event.register(tool("demo.server")))
        );

        mod.prepareServer();

        assertTrue(server.registry().findTool("demo.common").isPresent());
        assertTrue(server.registry().findTool("demo.client").isEmpty());
        assertTrue(server.registry().findTool("demo.server").isPresent());
    }

    private static McpToolProvider tool(String name) {
        return registry -> registry.registerTool(
                new McpToolDefinition(name, name, name, Map.of(), Map.of(), List.of(), "either", false, false, "public", "public"),
                (context, arguments) -> ToolResult.success(Map.of("tool", name))
        );
    }
}
