package dev.vfyjxf.mcp.server.protocol;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.host.RuntimeSession;
import dev.vfyjxf.mcp.server.host.RuntimeToolDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpProtocolRuntimeDispatchTest {

    @Test
    void dynamicRuntimeToolsAreListedOnceAcrossClientAndServerSides() {
        var server = new ModDevMcpServer();
        server.runtimeRegistry().connect(
                new RuntimeSession("client-runtime", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(dynamicTool("moddev.game_close", "common", "client"))
        );
        server.runtimeRegistry().connect(
                new RuntimeSession("server-runtime", "server", List.of("common", "server"), List.of("server"), Map.of()),
                List.of(dynamicTool("moddev.game_close", "common", "server"))
        );
        var dispatcher = new McpProtocolDispatcher(server);

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 11,
                "method", "tools/list"
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        @SuppressWarnings("unchecked")
        var tools = (List<Map<String, Object>>) result.get("tools");
        var count = tools.stream().filter(tool -> "moddev.game_close".equals(tool.get("name"))).count();
        assertEquals(1L, count);
    }

    @Test
    void dynamicRuntimeToolsAreDispatchedThroughRuntimeCallQueueUsingTargetSide() {
        var server = new ModDevMcpServer();
        server.runtimeRegistry().connect(
                new RuntimeSession("client-runtime", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(dynamicTool("moddev.game_close", "common", "client"))
        );
        server.runtimeRegistry().connect(
                new RuntimeSession("server-runtime", "server", List.of("common", "server"), List.of("server"), Map.of()),
                List.of(dynamicTool("moddev.game_close", "common", "server"))
        );
        server.callScheduler().setInvoker((session, descriptor, arguments) -> ToolResult.success(Map.of(
                "tool", descriptor.definition().name(),
                "runtimeId", session.runtimeId(),
                "runtimeSide", session.runtimeSide(),
                "targetSide", arguments.get("targetSide")
        )));
        var dispatcher = new McpProtocolDispatcher(server);

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 31,
                "method", "tools/call",
                "params", Map.of(
                        "name", "moddev.game_close",
                        "arguments", Map.of("targetSide", "client")
                )
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        assertFalse((Boolean) result.get("isError"));
        @SuppressWarnings("unchecked")
        var structuredContent = (Map<String, Object>) result.get("structuredContent");
        assertEquals("client-runtime", structuredContent.get("runtimeId"));
        assertEquals("client", structuredContent.get("runtimeSide"));
    }

    @Test
    void commonRuntimeToolRequiresTargetSideWhenClientAndServerAreBothConnected() {
        var server = new ModDevMcpServer();
        server.runtimeRegistry().connect(
                new RuntimeSession("client-runtime", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(dynamicTool("moddev.game_close", "common", "client"))
        );
        server.runtimeRegistry().connect(
                new RuntimeSession("server-runtime", "server", List.of("common", "server"), List.of("server"), Map.of()),
                List.of(dynamicTool("moddev.game_close", "common", "server"))
        );
        var dispatcher = new McpProtocolDispatcher(server);

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 32,
                "method", "tools/call",
                "params", Map.of(
                        "name", "moddev.game_close",
                        "arguments", Map.of()
                )
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        assertTrue((Boolean) result.get("isError"));
        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) result.get("content");
        assertEquals("ambiguous_runtime_side: specify targetSide", content.getFirst().get("text"));
    }

    private RuntimeToolDescriptor dynamicTool(String name, String definitionSide, String runtimeSide) {
        return new RuntimeToolDescriptor(
                new McpToolDefinition(
                        name,
                        "Game Close",
                        "Dynamic runtime tool",
                        Map.of(
                                "type", "object",
                                "properties", Map.of("targetSide", Map.of("type", "string"))
                        ),
                        Map.of("type", "object"),
                        List.of("game"),
                        definitionSide,
                        false,
                        false,
                        "runtime",
                        "runtime"
                ),
                "common",
                runtimeSide,
                true,
                true
        );
    }
}
