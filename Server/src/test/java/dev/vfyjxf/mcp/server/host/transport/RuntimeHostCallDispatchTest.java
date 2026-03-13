package dev.vfyjxf.mcp.server.host.transport;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.host.RuntimeCallQueue;
import dev.vfyjxf.mcp.server.host.RuntimeRegistry;
import dev.vfyjxf.mcp.server.host.RuntimeToolDescriptor;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeHostCallDispatchTest {

    @Test
    void hostDispatchesRuntimeCallsAndReceivesRuntimeResults() throws Exception {
        var registry = new RuntimeRegistry();
        var scheduler = new RuntimeCallQueue(registry);
        try (var host = RuntimeHost.start(registry, "127.0.0.1", freePort(), scheduler);
             var socket = new Socket("127.0.0.1", host.port());
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            writeLine(writer, "{" +
                    "\"type\":\"runtime.hello\"," +
                    "\"runtimeId\":\"runtime-1\"," +
                    "\"runtimeSide\":\"client\"," +
                    "\"supportedScopes\":[\"common\",\"client\"]," +
                    "\"supportedSides\":[\"client\"]," +
                    "\"toolDescriptors\":[{" +
                    "\"name\":\"demo.echo\"," +
                    "\"title\":\"Echo\"," +
                    "\"description\":\"Echo tool\"," +
                    "\"inputSchema\":{\"type\":\"object\"}," +
                    "\"outputSchema\":{\"type\":\"object\"}," +
                    "\"tags\":[\"demo\"]," +
                    "\"side\":\"client\"," +
                    "\"requiresWorld\":false," +
                    "\"requiresPlayer\":false," +
                    "\"availability\":\"runtime\"," +
                    "\"exposurePolicy\":\"runtime\"," +
                    "\"scope\":\"client\"," +
                    "\"runtimeToolSide\":\"client\"," +
                    "\"requiresGame\":true," +
                    "\"mutating\":false}]," +
                    "\"state\":{}}"
            );
            readJsonLine(reader);
            waitUntil(() -> registry.state().gameConnected());
            var tool = registry.listDynamicTools().getFirst();

            var resultFuture = CompletableFuture.supplyAsync(() -> scheduler.call(tool, Map.of("message", "hello")));
            var call = readJsonLine(reader);
            assertEquals("runtime.call", call.get("type"));
            assertEquals("demo.echo", call.get("toolName"));
            @SuppressWarnings("unchecked")
            var arguments = (Map<String, Object>) call.get("arguments");
            assertEquals("hello", arguments.get("message"));

            writeLine(writer, "{" +
                    "\"type\":\"runtime.result\"," +
                    "\"callId\":\"" + call.get("callId") + "\"," +
                    "\"success\":true," +
                    "\"value\":{\"message\":\"hello\",\"handledBy\":\"runtime\"}}"
            );

            var result = resultFuture.get(5, TimeUnit.SECONDS);
            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            var payload = (Map<String, Object>) result.value();
            assertEquals("hello", payload.get("message"));
            assertEquals("runtime", payload.get("handledBy"));
        }
    }

    private int freePort() throws Exception {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
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

    private void waitUntil(Check check) throws Exception {
        var deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            if (check.ok()) {
                return;
            }
            Thread.sleep(25L);
        }
        throw new AssertionError("Timed out waiting for condition");
    }

    @FunctionalInterface
    private interface Check {
        boolean ok();
    }
}

