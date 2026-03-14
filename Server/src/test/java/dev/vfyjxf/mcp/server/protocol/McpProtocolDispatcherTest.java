package dev.vfyjxf.mcp.server.protocol;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpResource;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.host.RuntimeSession;
import dev.vfyjxf.mcp.server.host.RuntimeToolDescriptor;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpProtocolDispatcherTest {

    @Test
    void initializeReturnsServerInfoAndCapabilities() {
        var dispatcher = new McpProtocolDispatcher(new ModDevMcpServer());

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2025-11-05",
                        "clientInfo", Map.of("name", "codex", "version", "test")
                )
        )).orElseThrow();

        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals(1, response.get("id"));
        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        assertEquals("2025-11-05", result.get("protocolVersion"));
        assertEquals("moddev-mcp", ((Map<?, ?>) result.get("serverInfo")).get("name"));
        assertTrue(((Map<?, ?>) result.get("capabilities")).containsKey("tools"));
        assertTrue(((Map<?, ?>) result.get("capabilities")).containsKey("resources"));
        assertEquals(true, ((Map<?, ?>) ((Map<?, ?>) result.get("capabilities")).get("tools")).get("listChanged"));
    }

    @Test
    void toolsListReturnsRegisteredTools() {
        var server = demoServer();
        var dispatcher = new McpProtocolDispatcher(server);

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 2,
                "method", "tools/list"
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        @SuppressWarnings("unchecked")
        var tools = (List<Map<String, Object>>) result.get("tools");
        assertEquals(2, tools.size());
        assertEquals("moddev.status", tools.getFirst().get("name"));
        assertEquals("demo.echo", tools.get(1).get("name"));
        assertEquals("Echo", tools.get(1).get("title"));
        assertTrue(tools.get(1).containsKey("inputSchema"));
    }

    @Test
    void toolsListIncludesConnectedRuntimeTools() {
        var server = demoServer();
        server.runtimeRegistry().connect(
                new RuntimeSession("runtime-1", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(new RuntimeToolDescriptor(
                        new McpToolDefinition(
                                "moddev.ui.inspect",
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
                ))
        );
        var dispatcher = new McpProtocolDispatcher(server);

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 21,
                "method", "tools/list"
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        @SuppressWarnings("unchecked")
        var tools = (List<Map<String, Object>>) result.get("tools");
        var toolNames = tools.stream().map(tool -> String.valueOf(tool.get("name"))).toList();
        assertTrue(toolNames.contains("moddev.status"));
        assertTrue(toolNames.contains("demo.echo"));
        assertTrue(toolNames.contains("moddev.ui.inspect"));
    }

    @Test
    void callingUnknownRuntimeToolReturnsExplicitDisconnectedError() {
        var dispatcher = new McpProtocolDispatcher(new ModDevMcpServer());

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 22,
                "method", "tools/call",
                "params", Map.of(
                        "name", "moddev.ui.inspect",
                        "arguments", Map.of()
                )
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        assertEquals(true, result.get("isError"));
        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) result.get("content");
        assertEquals("game_not_connected", content.getFirst().get("text"));
    }

    @Test
    void statusToolReturnsExplicitDisconnectedState() {
        var dispatcher = new McpProtocolDispatcher(new ModDevMcpServer());

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 23,
                "method", "tools/call",
                "params", Map.of(
                        "name", "moddev.status",
                        "arguments", Map.of()
                )
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        assertEquals(false, result.get("isError"));
        @SuppressWarnings("unchecked")
        var structuredContent = (Map<String, Object>) result.get("structuredContent");
        assertEquals(true, structuredContent.get("hostReady"));
        assertEquals(false, structuredContent.get("gameConnected"));
        assertTrue(structuredContent.containsKey("connectedAgentCount"));
    }

    @Test
    void toolsCallReturnsStructuredSuccessPayload() {
        var server = demoServer();
        var dispatcher = new McpProtocolDispatcher(server);

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 3,
                "method", "tools/call",
                "params", Map.of(
                        "name", "demo.echo",
                        "arguments", Map.of("message", "hello")
                )
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        assertEquals(false, result.get("isError"));
        @SuppressWarnings("unchecked")
        var structuredContent = (Map<String, Object>) result.get("structuredContent");
        assertEquals("hello", structuredContent.get("message"));
        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) result.get("content");
        assertEquals("text", content.getFirst().get("type"));
        var serializedStructuredContent = String.valueOf(content.getFirst().get("text"));
        assertTrue(serializedStructuredContent.contains("\"message\":\"hello\""));
    }

    @Test
    void toolsCallReturnsStructuredErrorPayloadForToolFailure() {
        var registry = new McpToolRegistry();
        var server = new ModDevMcpServer(registry);
        registry.registerTool(
                demoToolDefinition(),
                (context, arguments) -> ToolResult.failure("boom")
        );
        var dispatcher = new McpProtocolDispatcher(server);

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 4,
                "method", "tools/call",
                "params", Map.of(
                        "name", "demo.echo",
                        "arguments", Map.of()
                )
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.get("result");
        assertEquals(true, result.get("isError"));
        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) result.get("content");
        assertEquals("text", content.getFirst().get("type"));
        assertEquals("boom", content.getFirst().get("text"));
    }

    @Test
    void resourcesListAndReadUseRegisteredProviders() {
        var server = demoServer();
        var dispatcher = new McpProtocolDispatcher(server);

        var listResponse = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 5,
                "method", "resources/list"
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var listResult = (Map<String, Object>) listResponse.get("result");
        @SuppressWarnings("unchecked")
        var resources = (List<Map<String, Object>>) listResult.get("resources");
        assertEquals(1, resources.size());
        assertEquals("moddev://capture/ui-1", resources.getFirst().get("uri"));

        var readResponse = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 6,
                "method", "resources/read",
                "params", Map.of("uri", "moddev://capture/ui-1")
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var readResult = (Map<String, Object>) readResponse.get("result");
        @SuppressWarnings("unchecked")
        var contents = (List<Map<String, Object>>) readResult.get("contents");
        assertEquals(1, contents.size());
        assertEquals("moddev://capture/ui-1", contents.getFirst().get("uri"));
        assertEquals(Base64.getEncoder().encodeToString("png".getBytes(StandardCharsets.UTF_8)), contents.getFirst().get("blob"));
    }

    @Test
    void initializedNotificationReturnsNoResponse() {
        var dispatcher = new McpProtocolDispatcher(new ModDevMcpServer());

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "method", "notifications/initialized"
        ));

        assertTrue(response.isEmpty());
    }

    @Test
    void unknownMethodReturnsJsonRpcMethodNotFoundError() {
        var dispatcher = new McpProtocolDispatcher(new ModDevMcpServer());

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 7,
                "method", "prompts/list"
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var error = (Map<String, Object>) response.get("error");
        assertEquals(-32601, error.get("code"));
        assertEquals("Method not found", error.get("message"));
    }

    @Test
    void invalidToolsCallArgumentsReturnInvalidParamsError() {
        var dispatcher = new McpProtocolDispatcher(demoServer());

        var response = dispatcher.handle(Map.of(
                "jsonrpc", "2.0",
                "id", 8,
                "method", "tools/call",
                "params", Map.of("name", "demo.echo", "arguments", "bad-shape")
        )).orElseThrow();

        @SuppressWarnings("unchecked")
        var error = (Map<String, Object>) response.get("error");
        assertEquals(-32602, error.get("code"));
        assertEquals("Invalid params", error.get("message"));
        assertInstanceOf(Map.class, error.get("data"));
        assertFalse(((Map<?, ?>) error.get("data")).isEmpty());
    }

    private ModDevMcpServer demoServer() {
        var registry = new McpToolRegistry();
        var server = new ModDevMcpServer(registry);
        registry.registerTool(
                demoToolDefinition(),
                (context, arguments) -> ToolResult.success(Map.of("message", arguments.get("message")))
        );
        server.registerResourceProvider(new DemoResourceProvider());
        return server;
    }

    private McpToolDefinition demoToolDefinition() {
        return new McpToolDefinition(
                "demo.echo",
                "Echo",
                "Echoes the input",
                Map.of("type", "object"),
                Map.of("type", "object"),
                List.of("demo"),
                "either",
                false,
                false,
                "public",
                "public"
        );
    }

    private static final class DemoResourceProvider implements dev.vfyjxf.mcp.server.api.McpResourceProvider {

        @Override
        public java.util.Optional<McpResource> read(String uri) {
            return uri.equals("moddev://capture/ui-1")
                    ? java.util.Optional.of(new McpResource(
                    uri,
                    "image/png",
                    "Capture ui-1",
                    Map.of("width", 32),
                    "png".getBytes(StandardCharsets.UTF_8)
            ))
                    : java.util.Optional.empty();
        }

        @Override
        public List<McpResource> list() {
            return List.of(new McpResource(
                    "moddev://capture/ui-1",
                    "image/png",
                    "Capture ui-1",
                    Map.of("width", 32),
                    "png".getBytes(StandardCharsets.UTF_8)
            ));
        }
    }
}

