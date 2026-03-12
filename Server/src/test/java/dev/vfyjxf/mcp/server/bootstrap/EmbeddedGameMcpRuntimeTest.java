package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import dev.vfyjxf.mcp.server.transport.JsonCodec;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedGameMcpRuntimeTest {

    @Test
    void embeddedRuntimeStartsSocketHostAndServesRegisteredTools() throws Exception {
        var registry = new McpToolRegistry();
        var server = new ModDevMcpServer(registry);
        registry.registerTool(
                new McpToolDefinition(
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
                ),
                (context, arguments) -> ToolResult.success(arguments)
        );
        var config = new EmbeddedGameMcpConfig("127.0.0.1", freePort());

        try (var runtime = EmbeddedGameMcpRuntime.start(server, config);
             var socket = new Socket(config.host(), config.port());
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            writeJsonLine(writer, """
                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05","capabilities":{},"clientInfo":{"name":"embedded-server-test","version":"0.0.0"}}}
                    """);
            writeJsonLine(writer, """
                    {"jsonrpc":"2.0","method":"notifications/initialized"}
                    """);
            writeJsonLine(writer, """
                    {"jsonrpc":"2.0","id":2,"method":"tools/list"}
                    """);

            var initialize = parseJsonLine(reader);
            var toolsList = parseJsonLine(reader);

            @SuppressWarnings("unchecked")
            var initializeResult = (Map<String, Object>) initialize.get("result");
            assertEquals("2025-11-05", initializeResult.get("protocolVersion"));
            assertEquals(config.port(), runtime.port());

            @SuppressWarnings("unchecked")
            var toolsListResult = (Map<String, Object>) toolsList.get("result");
            var toolsRaw = String.valueOf(toolsListResult.get("tools"));
            assertTrue(toolsRaw.contains("demo.echo"), toolsRaw);
        }
    }

    private int freePort() throws Exception {
        try (var serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    private void writeJsonLine(BufferedWriter writer, String json) throws Exception {
        writer.write(json.strip());
        writer.newLine();
        writer.flush();
    }

    private Map<String, Object> parseJsonLine(BufferedReader reader) throws Exception {
        var deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            if (reader.ready()) {
                return new JsonCodec().parseObject(reader.readLine().getBytes(StandardCharsets.UTF_8));
            }
            Thread.sleep(25L);
        }
        throw new AssertionError("Timed out waiting for JSON line response");
    }
}
