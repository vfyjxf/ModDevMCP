package dev.vfyjxf.moddev.server.transport;

import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GsonMcpJsonMapperTest {

    @Test
    void convertValueSupportsSdkTypeRefRequests() {
        var mapper = new GsonMcpJsonMapper();

        var request = mapper.convertValue(
                Map.of(
                        "name", "demo.echo",
                        "arguments", Map.of("message", "hello")
                ),
                new TypeRef<McpSchema.CallToolRequest>() {
                }
        );

        assertEquals("demo.echo", request.name());
        assertEquals("hello", request.arguments().get("message"));
    }

    @Test
    void writeValueAsStringSerializesJsonRpcResponse() throws IOException {
        var mapper = new GsonMcpJsonMapper();

        var json = mapper.writeValueAsString(new McpSchema.JSONRPCResponse(
                "2.0",
                1,
                Map.of("ok", true),
                null
        ));

        assertTrue(json.contains("\"jsonrpc\":\"2.0\""));
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"ok\":true"));
    }

    @Test
    void deserializeJsonRpcMessageBuildsRequestRecords() throws IOException {
        var mapper = new GsonMcpJsonMapper();

        var message = McpSchema.deserializeJsonRpcMessage(
                mapper,
                """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18"}}
                """
        );

        assertEquals(McpSchema.JSONRPCRequest.class, message.getClass());
        assertEquals("initialize", ((McpSchema.JSONRPCRequest) message).method());
    }

    @Test
    void convertValueBuildsInitializeRequestFromCodexShape() {
        var mapper = new GsonMcpJsonMapper();

        var request = mapper.convertValue(
                Map.of(
                        "protocolVersion", "2025-06-18",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of(
                                "name", "codex",
                                "version", "0.0.0"
                        )
                ),
                McpSchema.InitializeRequest.class
        );

        assertEquals("2025-06-18", request.protocolVersion());
        assertEquals("codex", request.clientInfo().name());
        assertEquals("0.0.0", request.clientInfo().version());
    }
}

