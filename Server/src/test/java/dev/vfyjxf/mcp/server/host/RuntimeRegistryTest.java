package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeRegistryTest {

    @Test
    void startsDisconnectedWithNoRuntimeTools() {
        var registry = new RuntimeRegistry();

        assertFalse(registry.state().gameConnected());
        assertTrue(registry.activeSession().isEmpty());
        assertTrue(registry.listDynamicTools().isEmpty());
    }

    @Test
    void connectRegistersActiveRuntimeSessionAndDynamicTools() {
        var registry = new RuntimeRegistry();
        var session = new RuntimeSession(
                "runtime-1",
                "client",
                List.of("common", "client"),
                List.of("client"),
                Map.of("screenClass", "net.minecraft.client.gui.screens.TitleScreen")
        );
        var tool = descriptor("moddev.ui.inspect", "client");

        registry.connect(session, List.of(tool));

        assertTrue(registry.state().gameConnected());
        assertSame(session, registry.activeSession().orElseThrow());
        assertEquals(List.of(tool), registry.listDynamicTools());
    }

    @Test
    void connectReplacesPreviousRuntimeSessionAndDescriptors() {
        var registry = new RuntimeRegistry();
        var first = new RuntimeSession("runtime-1", "client", List.of("client"), List.of("client"), Map.of());
        var second = new RuntimeSession("runtime-2", "client", List.of("common", "client"), List.of("client"), Map.of("worldLoaded", true));
        var firstTool = descriptor("moddev.ui.inspect", "client");
        var secondTool = descriptor("moddev.status.client", "common");

        registry.connect(first, List.of(firstTool));
        registry.connect(second, List.of(secondTool));

        assertEquals("runtime-2", registry.activeSession().orElseThrow().runtimeId());
        assertEquals(List.of(secondTool), registry.listDynamicTools());
    }

    @Test
    void disconnectClearsRuntimeSessionAndDescriptors() {
        var registry = new RuntimeRegistry();
        registry.connect(
                new RuntimeSession("runtime-1", "client", List.of("client"), List.of("client"), Map.of()),
                List.of(descriptor("moddev.ui.inspect", "client"))
        );

        registry.disconnect("runtime-1");

        assertFalse(registry.state().gameConnected());
        assertTrue(registry.activeSession().isEmpty());
        assertTrue(registry.listDynamicTools().isEmpty());
    }

    @Test
    void refreshToolsKeepsSessionButReplacesDescriptors() {
        var registry = new RuntimeRegistry();
        registry.connect(
                new RuntimeSession("runtime-1", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(descriptor("moddev.ui.inspect", "client"))
        );
        var refreshed = descriptor("moddev.ui.capture", "client");

        registry.refreshTools("runtime-1", List.of(refreshed));

        assertTrue(registry.state().gameConnected());
        assertEquals("runtime-1", registry.activeSession().orElseThrow().runtimeId());
        assertEquals(List.of(refreshed), registry.listDynamicTools());
    }

    private RuntimeToolDescriptor descriptor(String toolName, String scope) {
        return new RuntimeToolDescriptor(
                new McpToolDefinition(
                        toolName,
                        toolName,
                        "dynamic tool",
                        Map.of("type", "object"),
                        Map.of("type", "object"),
                        List.of("runtime"),
                        "client",
                        false,
                        false,
                        "runtime",
                        "runtime"
                ),
                scope,
                "client",
                true,
                false
        );
    }
}

