package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UiToolProviderTest {

    @Test
    void uiProviderRegistersExpectedToolNames() {
        var registry = new McpToolRegistry();
        new UiToolProvider(new RuntimeRegistries()).register(registry);

        assertTrue(registry.findTool("moddev.ui_snapshot").isPresent());
        assertTrue(registry.findTool("moddev.ui_capture").isPresent());
        assertTrue(registry.findTool("moddev.ui_get_interaction_state").isPresent());
    }
}
