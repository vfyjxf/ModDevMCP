package dev.vfyjxf.mcp.server.host.protocol;

import dev.vfyjxf.mcp.server.host.RuntimeRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeHostDispatcherTest {

    @Test
    void helloRegistersRuntimeSessionAndTools() {
        var registry = new RuntimeRegistry();
        var dispatcher = new RuntimeHostDispatcher(registry);

        var response = dispatcher.handle(Map.of(
                "type", "runtime.hello",
                "runtimeId", "runtime-1",
                "runtimeSide", "client",
                "supportedScopes", List.of("common", "client"),
                "supportedSides", List.of("client"),
                "toolDescriptors", List.of(toolDescriptor("moddev.ui.inspect", "Inspect UI")),
                "state", Map.of("screenClass", "net.minecraft.client.gui.screens.TitleScreen")
        ));

        assertEquals("ok", response.get("status"));
        assertTrue(registry.state().gameConnected());
        assertEquals("runtime-1", registry.activeSession().orElseThrow().runtimeId());
        assertEquals(1, registry.listDynamicTools().size());
        assertEquals("moddev.ui.inspect", registry.listDynamicTools().getFirst().definition().name());
    }

    @Test
    void refreshReplacesRegisteredRuntimeTools() {
        var registry = new RuntimeRegistry();
        var dispatcher = new RuntimeHostDispatcher(registry);
        dispatcher.handle(Map.of(
                "type", "runtime.hello",
                "runtimeId", "runtime-1",
                "runtimeSide", "client",
                "supportedScopes", List.of("common", "client"),
                "supportedSides", List.of("client"),
                "toolDescriptors", List.of(toolDescriptor("moddev.ui.inspect", "Inspect UI")),
                "state", Map.of()
        ));

        var response = dispatcher.handle(Map.of(
                "type", "runtime.refresh",
                "runtimeId", "runtime-1",
                "toolDescriptors", List.of(toolDescriptor("moddev.ui.capture", "Capture UI"))
        ));

        assertEquals("ok", response.get("status"));
        assertEquals(1, registry.listDynamicTools().size());
        assertEquals("moddev.ui.capture", registry.listDynamicTools().getFirst().definition().name());
    }

    @Test
    void goodbyeDisconnectsRuntimeSession() {
        var registry = new RuntimeRegistry();
        var dispatcher = new RuntimeHostDispatcher(registry);
        dispatcher.handle(Map.of(
                "type", "runtime.hello",
                "runtimeId", "runtime-1",
                "runtimeSide", "client",
                "supportedScopes", List.of("common", "client"),
                "supportedSides", List.of("client"),
                "toolDescriptors", List.of(),
                "state", Map.of()
        ));

        var response = dispatcher.handle(Map.of(
                "type", "runtime.goodbye",
                "runtimeId", "runtime-1"
        ));

        assertEquals("ok", response.get("status"));
        assertFalse(registry.state().gameConnected());
    }

    private Map<String, Object> toolDescriptor(String name, String title) {
        return Map.ofEntries(
                Map.entry("name", name),
                Map.entry("title", title),
                Map.entry("description", "Dynamic runtime tool"),
                Map.entry("inputSchema", Map.of("type", "object")),
                Map.entry("outputSchema", Map.of("type", "object")),
                Map.entry("tags", List.of("ui")),
                Map.entry("side", "client"),
                Map.entry("requiresWorld", false),
                Map.entry("requiresPlayer", false),
                Map.entry("availability", "runtime"),
                Map.entry("exposurePolicy", "runtime"),
                Map.entry("scope", "client"),
                Map.entry("runtimeToolSide", "client"),
                Map.entry("requiresGame", true),
                Map.entry("mutating", false)
        );
    }
}

