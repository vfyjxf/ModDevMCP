package dev.vfyjxf.moddev.server.protocol;

import dev.vfyjxf.moddev.server.ModDevMcpServer;
import dev.vfyjxf.moddev.server.api.McpToolDefinition;
import dev.vfyjxf.moddev.server.api.ToolResult;
import dev.vfyjxf.moddev.server.host.RuntimeSession;
import dev.vfyjxf.moddev.server.host.RuntimeToolDescriptor;
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
                List.of(dynamicTool("moddev.game_close", "common", "client"), dynamicTool("moddev.command_execute", "common", "client"))
        );
        server.runtimeRegistry().connect(
                new RuntimeSession("server-runtime", "server", List.of("common", "server"), List.of("server"), Map.of()),
                List.of(dynamicTool("moddev.game_close", "common", "server"), dynamicTool("moddev.command_execute", "common", "server"))
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
        var commandCount = tools.stream().filter(tool -> "moddev.command_execute".equals(tool.get("name"))).count();
        assertEquals(1L, commandCount);
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
        server.callScheduler().setInvoker((session, descriptor, arguments) -> {
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("tool", descriptor.definition().name());
            payload.put("runtimeId", session.runtimeId());
            payload.put("runtimeSide", session.runtimeSide());
            if (arguments.containsKey("targetSide")) {
                payload.put("targetSide", arguments.get("targetSide"));
            }
            return ToolResult.success(Map.copyOf(payload));
        });
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

    @Test
    void commandToolsUseRuntimeSideForRuntimeRoutingAndPreserveTargetSide() {
        var server = new ModDevMcpServer();
        server.runtimeRegistry().connect(
                new RuntimeSession("client-runtime", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(dynamicTool("moddev.command_execute", "common", "client"))
        );
        server.callScheduler().setInvoker((session, descriptor, arguments) -> {
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("tool", descriptor.definition().name());
            payload.put("runtimeId", session.runtimeId());
            payload.put("runtimeSide", session.runtimeSide());
            if (arguments.containsKey("targetSide")) {
                payload.put("targetSide", arguments.get("targetSide"));
            }
            if (arguments.containsKey("runtimeSide")) {
                payload.put("runtimeSideArg", arguments.get("runtimeSide"));
            }
            return ToolResult.success(Map.copyOf(payload));
        });
        var dispatcher = new McpProtocolDispatcher(server);

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 33,
                "method", "tools/call",
                "params", Map.of(
                        "name", "moddev.command_execute",
                        "arguments", Map.of(
                                "runtimeSide", "client",
                                "targetSide", "server",
                                "command", "time set day"
                        )
                )
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        assertFalse((Boolean) result.get("isError"));
        @SuppressWarnings("unchecked")
        var structuredContent = (Map<String, Object>) result.get("structuredContent");
        assertEquals("client-runtime", structuredContent.get("runtimeId"));
        assertEquals("client", structuredContent.get("runtimeSide"));
        assertEquals("server", structuredContent.get("targetSide"));
        assertEquals(null, structuredContent.get("runtimeSideArg"));
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

