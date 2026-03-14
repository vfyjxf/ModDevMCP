package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeRegistryTest {

    @Test
    void startsDisconnectedWithNoRuntimeTools() {
        var registry = new RuntimeRegistry();

        assertFalse(registry.state().gameConnected());
        assertTrue(registry.listSessions().isEmpty());
        assertTrue(registry.listDynamicTools().isEmpty());
    }

    @Test
    void connectKeepsClientAndServerSessionsAtTheSameTime() {
        var registry = new RuntimeRegistry();
        var clientSession = new RuntimeSession(
                "client-runtime",
                "client",
                List.of("common", "client"),
                List.of("client"),
                Map.of("screenClass", "net.minecraft.client.gui.screens.TitleScreen")
        );
        var serverSession = new RuntimeSession(
                "server-runtime",
                "server",
                List.of("common", "server"),
                List.of("server"),
                Map.of("worldLoaded", true)
        );

        registry.connect(clientSession, List.of(descriptor("moddev.game_close", "common", "client", "common")));
        registry.connect(serverSession, List.of(descriptor("moddev.game_close", "common", "server", "common")));

        assertTrue(registry.state().gameConnected());
        assertEquals(2, registry.listSessions().size());
        assertTrue(registry.findSession("client-runtime").isPresent());
        assertTrue(registry.findSession("server-runtime").isPresent());
        assertEquals(1, registry.listDynamicTools().size());
        assertEquals("moddev.game_close", registry.listDynamicTools().getFirst().definition().name());
    }

    @Test
    void disconnectRemovesOnlyMatchingRuntimeAndKeepsOtherSideConnected() {
        var registry = new RuntimeRegistry();
        registry.connect(
                new RuntimeSession("client-runtime", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(descriptor("moddev.game_close", "common", "client", "common"))
        );
        registry.connect(
                new RuntimeSession("server-runtime", "server", List.of("common", "server"), List.of("server"), Map.of()),
                List.of(descriptor("moddev.game_close", "common", "server", "common"))
        );

        registry.disconnect("client-runtime");

        assertTrue(registry.state().gameConnected());
        assertEquals(1, registry.listSessions().size());
        assertTrue(registry.findSession("server-runtime").isPresent());
        assertFalse(registry.findSession("client-runtime").isPresent());
        assertEquals(1, registry.listDynamicTools().size());
    }

    @Test
    void refreshToolsUpdatesOnlyMatchingRuntimeWithoutDroppingOtherRuntimeDescriptors() {
        var registry = new RuntimeRegistry();
        registry.connect(
                new RuntimeSession("client-runtime", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(descriptor("moddev.ui_snapshot", "client", "client", "client"))
        );
        registry.connect(
                new RuntimeSession("server-runtime", "server", List.of("common", "server"), List.of("server"), Map.of()),
                List.of(descriptor("moddev.game_close", "common", "server", "common"))
        );

        registry.refreshTools("client-runtime", List.of(descriptor("moddev.ui_capture", "client", "client", "client")));

        var toolNames = registry.listDynamicTools().stream().map(tool -> tool.definition().name()).toList();
        assertEquals(List.of("moddev.game_close", "moddev.ui_capture"), toolNames.stream().sorted().toList());
    }

    private RuntimeToolDescriptor descriptor(String toolName, String scope, String runtimeSide, String definitionSide) {
        return new RuntimeToolDescriptor(
                new McpToolDefinition(
                        toolName,
                        toolName,
                        "dynamic tool",
                        Map.of("type", "object"),
                        Map.of("type", "object"),
                        List.of("runtime"),
                        definitionSide,
                        false,
                        false,
                        "runtime",
                        "runtime"
                ),
                scope,
                runtimeSide,
                true,
                false
        );
    }
}
