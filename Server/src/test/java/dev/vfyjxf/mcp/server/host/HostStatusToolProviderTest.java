package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostStatusToolProviderTest {

    @Test
    void statusToolIsAlwaysRegisteredOnServer() {
        var server = new ModDevMcpServer();

        assertTrue(server.registry().findTool("moddev.status").isPresent());
    }

    @Test
    void statusToolReflectsClientAndServerRuntimeConnectionState() {
        var server = new ModDevMcpServer();
        server.runtimeRegistry().connect(
                new RuntimeSession("client-runtime", "client", List.of("common", "client"), List.of("client"), Map.of("worldLoaded", true)),
                List.of()
        );
        server.runtimeRegistry().connect(
                new RuntimeSession("server-runtime", "server", List.of("common", "server"), List.of("server"), Map.of("worldLoaded", true)),
                List.of()
        );

        var result = server.registry().findTool("moddev.status").orElseThrow()
                .handler()
                .handle(ToolCallContext.empty(), Map.of());

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertTrue((Boolean) payload.get("hostReady"));
        assertTrue((Boolean) payload.get("gameConnected"));
        assertFalse((Boolean) payload.get("gameConnecting"));
        assertTrue((Boolean) payload.get("clientConnected"));
        assertTrue((Boolean) payload.get("serverConnected"));
        @SuppressWarnings("unchecked")
        var connectedRuntimes = (List<Map<String, Object>>) payload.get("connectedRuntimes");
        assertEquals(2, connectedRuntimes.size());
    }
}
