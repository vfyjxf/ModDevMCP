package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.game.GameCloser;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameToolProviderTest {

    @Test
    void gameCloseToolDefinesCommonSchema() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        new GameToolProvider(() -> true).register(server.registry());

        var definition = server.registry().findTool("moddev.game_close").orElseThrow().definition();

        assertEquals("common", definition.side());
        assertEquals(List.of("game", "lifecycle"), definition.tags());
        assertEquals("object", definition.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) definition.inputSchema().get("properties")).containsKey("targetSide"));
        assertEquals("object", definition.outputSchema().get("type"));
        assertEquals(List.of("accepted", "runtimeId", "runtimeSide"), definition.outputSchema().get("required"));
    }

    @Test
    void gameCloseToolRequestsShutdownAndReturnsAcceptedPayloadForClientRuntime() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var closer = new RecordingGameCloser(true);
        new GameToolProvider(closer).register(server.registry());

        var tool = server.registry().findTool("moddev.game_close").orElseThrow();
        var result = tool.handler().handle(new ToolCallContext("client", Map.of("runtimeId", "client-runtime")), Map.of("targetSide", "client"));

        assertTrue(result.success());
        assertEquals(1, closer.calls.get());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(true, payload.get("accepted"));
        assertEquals("client-runtime", payload.get("runtimeId"));
        assertEquals("client", payload.get("runtimeSide"));
    }

    @Test
    void gameCloseToolRequestsShutdownAndReturnsAcceptedPayloadForServerRuntime() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var closer = new RecordingGameCloser(true);
        new GameToolProvider(closer).register(server.registry());

        var tool = server.registry().findTool("moddev.game_close").orElseThrow();
        var result = tool.handler().handle(new ToolCallContext("server", Map.of("runtimeId", "server-runtime")), Map.of("targetSide", "server"));

        assertTrue(result.success());
        assertEquals(1, closer.calls.get());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(true, payload.get("accepted"));
        assertEquals("server-runtime", payload.get("runtimeId"));
        assertEquals("server", payload.get("runtimeSide"));
    }

    @Test
    void gameCloseToolReturnsFailureWhenRuntimeRejectsShutdown() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        new GameToolProvider(() -> false).register(server.registry());

        var tool = server.registry().findTool("moddev.game_close").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of());

        assertFalse(result.success());
        assertEquals("game_close_rejected", result.error());
    }

    private record RecordingGameCloser(AtomicInteger calls, boolean result) implements GameCloser {

        private RecordingGameCloser(boolean result) {
            this(new AtomicInteger(), result);
        }

        @Override
        public boolean requestClose() {
            calls.incrementAndGet();
            return result;
        }
    }
}
