package dev.vfyjxf.mcp.server.protocol;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.host.RuntimeSession;
import dev.vfyjxf.mcp.server.host.RuntimeToolDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class McpProtocolRuntimeDispatchTest {

    @Test
    void dynamicRuntimeToolsAreDispatchedThroughRuntimeCallQueue() {
        var server = new ModDevMcpServer();
        server.runtimeRegistry().connect(
                new RuntimeSession("runtime-1", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(dynamicTool("moddev.ui.inspect"))
        );
        server.callScheduler().setInvoker((session, descriptor, arguments) -> ToolResult.success(Map.of(
                "tool", descriptor.definition().name(),
                "message", arguments.get("message")
        )));
        var dispatcher = new McpProtocolDispatcher(server);

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 31,
                "method", "tools/call",
                "params", Map.of(
                        "name", "moddev.ui.inspect",
                        "arguments", Map.of("message", "hello")
                )
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        assertFalse((Boolean) result.get("isError"));
        @SuppressWarnings("unchecked")
        var structuredContent = (Map<String, Object>) result.get("structuredContent");
        assertEquals("moddev.ui.inspect", structuredContent.get("tool"));
        assertEquals("hello", structuredContent.get("message"));
    }

    private RuntimeToolDescriptor dynamicTool(String name) {
        return new RuntimeToolDescriptor(
                new McpToolDefinition(
                        name,
                        "Inspect UI",
                        "Dynamic runtime tool",
                        Map.of("type", "object"),
                        Map.of("type", "object"),
                        List.of("ui"),
                        "client",
                        false,
                        false,
                        "runtime",
                        "runtime"
                ),
                "client",
                "client",
                true,
                false
        );
    }
}

