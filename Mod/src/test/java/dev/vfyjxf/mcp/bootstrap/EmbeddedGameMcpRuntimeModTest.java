package dev.vfyjxf.mcp.bootstrap;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.bootstrap.EmbeddedGameMcpConfig;
import dev.vfyjxf.mcp.server.bootstrap.EmbeddedGameMcpRuntime;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedGameMcpRuntimeModTest {

    @Test
    void embeddedRuntimeServesBuiltinModTools() throws Exception {
        var mod = new ModDevMCP(new ModDevMcpServer());
        var config = new EmbeddedGameMcpConfig("127.0.0.1", freePort());

        try (var runtime = EmbeddedGameMcpRuntime.start(mod.prepareServer(), config);
             var socket = new Socket(config.host(), config.port());
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            writeJsonLine(writer, """
                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05","capabilities":{},"clientInfo":{"name":"game-hosted-test","version":"0.0.0"}}}
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
            assertTrue(toolsRaw.contains("moddev.ui_snapshot"), toolsRaw);
            assertTrue(toolsRaw.contains("moddev.ui_get_live_screen"), toolsRaw);
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
