package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiToolProviderTest {

    @Test
    void uiProviderRegistersExpectedToolNames() {
        var registry = new McpToolRegistry();
        new UiToolProvider(new RuntimeRegistries()).register(registry);

        assertTrue(registry.findTool("moddev.ui_run_intent").isPresent());
        assertTrue(registry.findTool("moddev.ui_snapshot").isPresent());
        assertTrue(registry.findTool("moddev.ui_capture").isPresent());
        assertTrue(registry.findTool("moddev.ui_get_interaction_state").isPresent());
        assertTrue(registry.findTool("moddev.ui_get_live_screen").isPresent());
        assertTrue(registry.findTool("moddev.ui_session_open").isPresent());
        assertTrue(registry.findTool("moddev.ui_session_refresh").isPresent());
        assertTrue(registry.findTool("moddev.ui_click_ref").isPresent());
        assertTrue(registry.findTool("moddev.ui_hover_ref").isPresent());
        assertTrue(registry.findTool("moddev.ui_press_key").isPresent());
        assertTrue(registry.findTool("moddev.ui_type_text").isPresent());
        assertTrue(registry.findTool("moddev.ui_wait_for").isPresent());
        assertTrue(registry.findTool("moddev.ui_screenshot").isPresent());
    }

    @Test
    void uiProviderDefinesSchemaForCoreUiTools() {
        var registry = new McpToolRegistry();
        new UiToolProvider(new RuntimeRegistries()).register(registry);

        var snapshot = registry.findTool("moddev.ui_snapshot").orElseThrow().definition();
        var capture = registry.findTool("moddev.ui_capture").orElseThrow().definition();
        var wait = registry.findTool("moddev.ui_wait").orElseThrow().definition();
        var inspectAt = registry.findTool("moddev.ui_inspect_at").orElseThrow().definition();
        var tooltip = registry.findTool("moddev.ui_get_tooltip").orElseThrow().definition();
        var interactionState = registry.findTool("moddev.ui_get_interaction_state").orElseThrow().definition();
        var liveScreen = registry.findTool("moddev.ui_get_live_screen").orElseThrow().definition();
        var sessionOpen = registry.findTool("moddev.ui_session_open").orElseThrow().definition();
        var sessionRefresh = registry.findTool("moddev.ui_session_refresh").orElseThrow().definition();
        var clickRef = registry.findTool("moddev.ui_click_ref").orElseThrow().definition();
        var hoverRef = registry.findTool("moddev.ui_hover_ref").orElseThrow().definition();
        var pressKey = registry.findTool("moddev.ui_press_key").orElseThrow().definition();
        var typeText = registry.findTool("moddev.ui_type_text").orElseThrow().definition();
        var waitFor = registry.findTool("moddev.ui_wait_for").orElseThrow().definition();
        var screenshot = registry.findTool("moddev.ui_screenshot").orElseThrow().definition();
        var targetDetails = registry.findTool("moddev.ui_get_target_details").orElseThrow().definition();
        var runIntent = registry.findTool("moddev.ui_run_intent").orElseThrow().definition();
        var close = registry.findTool("moddev.ui_close").orElseThrow().definition();
        var uiSwitch = registry.findTool("moddev.ui_switch").orElseThrow().definition();

        assertEquals("object", snapshot.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) snapshot.inputSchema().get("properties")).containsKey("screenClass"));
        assertTrue(((Map<?, ?>) snapshot.outputSchema().get("properties")).containsKey("snapshotRef"));

        assertEquals("object", capture.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) capture.inputSchema().get("properties")).containsKey("source"));
        assertTrue(((Map<?, ?>) capture.outputSchema().get("properties")).containsKey("imageRef"));

        assertEquals("object", wait.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) wait.inputSchema().get("properties")).containsKey("condition"));
        assertEquals(List.of("driverId", "matched", "timedOut", "elapsedMs", "targets"), wait.outputSchema().get("required"));

        assertEquals("object", inspectAt.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) inspectAt.inputSchema().get("properties")).containsKey("x"));
        assertTrue(((Map<?, ?>) inspectAt.outputSchema().get("properties")).containsKey("topmostTarget"));

        assertEquals("object", tooltip.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) tooltip.inputSchema().get("properties")).containsKey("target"));
        assertTrue(((Map<?, ?>) tooltip.outputSchema().get("properties")).containsKey("lines"));

        assertEquals("object", interactionState.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) interactionState.outputSchema().get("properties")).containsKey("focusedTarget"));
        assertTrue(((Map<?, ?>) interactionState.outputSchema().get("properties")).containsKey("selectionSource"));

        assertEquals("object", liveScreen.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) liveScreen.outputSchema().get("properties")).containsKey("screenClass"));
        assertTrue(((Map<?, ?>) liveScreen.outputSchema().get("properties")).containsKey("active"));

        assertEquals("object", sessionOpen.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) sessionOpen.outputSchema().get("properties")).containsKey("sessionId"));
        assertTrue(((Map<?, ?>) sessionOpen.outputSchema().get("properties")).containsKey("refs"));

        assertEquals("object", sessionRefresh.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) sessionRefresh.inputSchema().get("properties")).containsKey("sessionId"));
        assertTrue(((Map<?, ?>) sessionRefresh.outputSchema().get("properties")).containsKey("screenChanged"));

        assertEquals("object", clickRef.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) clickRef.inputSchema().get("properties")).containsKey("sessionId"));
        assertTrue(((Map<?, ?>) clickRef.inputSchema().get("properties")).containsKey("refId"));
        assertTrue(((Map<?, ?>) clickRef.outputSchema().get("properties")).containsKey("postActionSnapshot"));

        assertEquals("object", hoverRef.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) hoverRef.inputSchema().get("properties")).containsKey("hoverDelayMs"));
        assertTrue(((Map<?, ?>) hoverRef.outputSchema().get("properties")).containsKey("action"));

        assertEquals("object", pressKey.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) pressKey.inputSchema().get("properties")).containsKey("keyCode"));
        assertTrue(((Map<?, ?>) pressKey.outputSchema().get("properties")).containsKey("controller"));

        assertEquals("object", typeText.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) typeText.inputSchema().get("properties")).containsKey("text"));
        assertTrue(((Map<?, ?>) typeText.outputSchema().get("properties")).containsKey("controller"));

        assertEquals("object", waitFor.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) waitFor.inputSchema().get("properties")).containsKey("sessionId"));
        assertTrue(((Map<?, ?>) waitFor.outputSchema().get("properties")).containsKey("matched"));

        assertEquals("object", screenshot.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) screenshot.inputSchema().get("properties")).containsKey("sessionId"));
        assertTrue(((Map<?, ?>) screenshot.outputSchema().get("properties")).containsKey("imageRef"));

        assertEquals("object", targetDetails.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) targetDetails.inputSchema().get("properties")).containsKey("target"));
        assertTrue(((Map<?, ?>) targetDetails.outputSchema().get("properties")).containsKey("hierarchyPath"));

        assertEquals("object", runIntent.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) runIntent.inputSchema().get("properties")).containsKey("intent"));
        assertTrue(((Map<?, ?>) runIntent.outputSchema().get("properties")).containsKey("intent"));
        assertTrue(((Map<?, ?>) runIntent.outputSchema().get("properties")).containsKey("performed"));

        assertEquals("object", close.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) close.inputSchema().get("properties")).containsKey("waitCondition"));
        assertTrue(((Map<?, ?>) close.outputSchema().get("properties")).containsKey("postSnapshotRef"));

        assertEquals("object", uiSwitch.inputSchema().get("type"));
        assertTrue(((Map<?, ?>) uiSwitch.inputSchema().get("properties")).containsKey("target"));
        assertTrue(((Map<?, ?>) uiSwitch.outputSchema().get("properties")).containsKey("wait"));
    }
}
