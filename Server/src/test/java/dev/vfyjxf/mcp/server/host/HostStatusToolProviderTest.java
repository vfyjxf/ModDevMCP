package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import org.junit.jupiter.api.Test;

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
    void statusToolReflectsRuntimeConnectionState() {
        var server = new ModDevMcpServer();
        server.runtimeRegistry().connect(
                new RuntimeSession("runtime-1", "client", java.util.List.of("common", "client"), java.util.List.of("client"), Map.of("worldLoaded", true)),
                java.util.List.of()
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
        assertEquals("runtime-1", payload.get("runtimeId"));
        assertEquals("client", payload.get("runtimeSide"));
    }
}

