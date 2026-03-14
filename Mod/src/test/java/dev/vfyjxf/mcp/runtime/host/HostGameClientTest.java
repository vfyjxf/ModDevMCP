package dev.vfyjxf.mcp.runtime.host;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostGameClientTest {

    @Test
    void clientSendsHelloAndHandlesRuntimeCalls() throws Exception {
        try (var relaySocket = new ServerSocket(0);
             var executor = Executors.newSingleThreadExecutor()) {
            var server = new ModDevMcpServer(new McpToolRegistry());
            server.registry().registerTool(
                    new McpToolDefinition(
                            "demo.echo",
                            "Echo",
                            "Echo tool",
                            Map.of("type", "object"),
                            Map.of("type", "object"),
                            List.of("demo"),
                            "client",
                            false,
                            false,
                            "runtime",
                            "runtime"
                    ),
                    (context, arguments) -> ToolResult.success(Map.of("message", arguments.get("message"), "handledBy", "game"))
            );
            var client = new HostGameClient(server, new HostRuntimeClientConfig("127.0.0.1", relaySocket.getLocalPort(), 50L), "runtime-1", "client");

            var runFuture = executor.submit(() -> { client.runUntilDisconnected(); return null; });
            try (var socket = relaySocket.accept();
                 var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                var hello = readJsonLine(reader);
                assertEquals("runtime.hello", hello.get("type"));
                assertEquals("runtime-1", hello.get("runtimeId"));
                @SuppressWarnings("unchecked")
                var descriptors = (List<Map<String, Object>>) hello.get("toolDescriptors");
                assertEquals("demo.echo", descriptors.getFirst().get("name"));
                writeLine(writer, "{\"status\":\"ok\"}");

                writeLine(writer, "{" +
                        "\"type\":\"runtime.call\"," +
                        "\"callId\":\"call-1\"," +
                        "\"toolName\":\"demo.echo\"," +
                        "\"arguments\":{\"message\":\"hello\"}}"
                );

                var result = readJsonLine(reader);
                assertEquals("runtime.result", result.get("type"));
                assertEquals("call-1", result.get("callId"));
                assertEquals(Boolean.TRUE, result.get("success"));
                @SuppressWarnings("unchecked")
                var value = (Map<String, Object>) result.get("value");
                assertEquals("hello", value.get("message"));
                assertEquals("game", value.get("handledBy"));
            }

            runFuture.get(5, TimeUnit.SECONDS);
            client.close();
        }
    }

    @Test
    void reconnectLoopRetriesUntilClientRunSucceeds() throws Exception {
        var attempts = new AtomicInteger();
        var connected = new CompletableFuture<Integer>();
        try (var loop = new HostReconnectLoop(() -> {
            var attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new java.io.IOException("refused");
            }
            connected.complete(attempt);
        }, 50L)) {
            loop.start();
            assertEquals(3, connected.get(5, TimeUnit.SECONDS));
        }
    }

    private void writeLine(BufferedWriter writer, String json) throws Exception {
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    private Map<String, Object> readJsonLine(BufferedReader reader) throws Exception {
        var deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            if (reader.ready()) {
                return new dev.vfyjxf.mcp.server.transport.JsonCodec().parseObject(reader.readLine().getBytes(StandardCharsets.UTF_8));
            }
            Thread.sleep(25L);
        }
        throw new AssertionError("Timed out waiting for JSON line");
    }
}

