package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.game.PauseOnLostFocusService;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PauseOnLostFocusToolProviderTest {

    @Test
    void providerRegistersPauseOnLostFocusTool() {
        var registry = new McpToolRegistry();
        new PauseOnLostFocusToolProvider(new RecordingPauseOnLostFocusService(false)).register(registry);

        assertTrue(registry.findTool("moddev.pause_on_lost_focus").isPresent());
    }

    @Test
    void providerDefinesClientSchema() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        new PauseOnLostFocusToolProvider(new RecordingPauseOnLostFocusService(false)).register(server.registry());

        var definition = server.registry().findTool("moddev.pause_on_lost_focus").orElseThrow().definition();

        assertEquals("client", definition.side());
        @SuppressWarnings("unchecked")
        var inputProperties = (Map<String, Object>) definition.inputSchema().get("properties");
        @SuppressWarnings("unchecked")
        var outputProperties = (Map<String, Object>) definition.outputSchema().get("properties");
        assertTrue(inputProperties.containsKey("enabled"));
        assertTrue(outputProperties.containsKey("enabled"));
        assertTrue(outputProperties.containsKey("changed"));
    }

    @Test
    void providerReturnsCurrentStateWhenEnabledIsMissing() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var service = new RecordingPauseOnLostFocusService(true);
        new PauseOnLostFocusToolProvider(service).register(server.registry());

        var result = server.registry().findTool("moddev.pause_on_lost_focus").orElseThrow()
                .handler()
                .handle(new dev.vfyjxf.mcp.server.api.ToolCallContext("client", Map.of("runtimeId", "client-runtime")), Map.of());

        assertTrue(result.success());
        assertEquals(0, service.setInvocations);
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(true, payload.get("enabled"));
        assertEquals(false, payload.get("changed"));
    }

    @Test
    void providerSetsAndReportsChangedState() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var service = new RecordingPauseOnLostFocusService(false);
        new PauseOnLostFocusToolProvider(service).register(server.registry());

        var result = server.registry().findTool("moddev.pause_on_lost_focus").orElseThrow()
                .handler()
                .handle(new dev.vfyjxf.mcp.server.api.ToolCallContext("client", Map.of("runtimeId", "client-runtime")), Map.of("enabled", true));

        assertTrue(result.success());
        assertEquals(1, service.setInvocations);
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(true, payload.get("enabled"));
        assertEquals(true, payload.get("changed"));
    }

    private static final class RecordingPauseOnLostFocusService implements PauseOnLostFocusService {
        private boolean enabled;
        private int setInvocations;

        private RecordingPauseOnLostFocusService(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public boolean currentState() {
            return enabled;
        }

        @Override
        public boolean setEnabled(boolean enabled) {
            boolean changed = this.enabled != enabled;
            this.enabled = enabled;
            setInvocations++;
            return changed;
        }
    }
}
