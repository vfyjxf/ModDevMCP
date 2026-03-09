package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.api.runtime.UiCaptureImage;
import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiFramebufferCaptureProvider;
import dev.vfyjxf.mcp.api.runtime.UiInteractionStateResolver;
import dev.vfyjxf.mcp.api.runtime.UiOffscreenCaptureProvider;
import dev.vfyjxf.mcp.api.ui.UiInteractionDefaults;
import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.runtime.ui.UiCaptureRenderer;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiToolInvocationTest {

    @Test
    void uiSnapshotToolReturnsDriverAndTargetIds() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_snapshot").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of("screenClass", "custom.UnknownScreen"));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("fallback-region", payload.get("driverId"));
        assertTrue(((List<?>) payload.get("targets")).size() >= 1);
    }

    @Test
    void uiQueryUsesSelectorToFilterTargets() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_query").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "selector", Map.of("role", "slot")
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("vanilla-container", payload.get("driverId"));
        assertEquals(1, ((List<?>) payload.get("targets")).size());
    }

    @Test
    void uiCaptureDelegatesToDriverCapture() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "mode", "crop"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("fallback-region", payload.get("driverId"));
        assertEquals("crop", payload.get("mode"));
        assertEquals(1, ((List<?>) payload.get("capturedTargets")).size());
        assertTrue(payload.containsKey("snapshotRef"));
        assertEquals("fallback-region", ((Map<?, ?>) payload.get("capturedSnapshot")).get("driverId"));
        assertTrue(payload.containsKey("imageRef"));
        assertTrue(payload.containsKey("imagePath"));
        assertTrue(payload.containsKey("imageResourceUri"));
        assertEquals("png", ((Map<?, ?>) payload.get("imageMeta")).get("format"));
        assertEquals("placeholder", ((Map<?, ?>) payload.get("imageMeta")).get("source"));
        assertTrue(Files.exists(Path.of((String) payload.get("imagePath"))));
        assertEquals("image/png", server.resourceRegistry().read((String) payload.get("imageResourceUri")).orElseThrow().mimeType());
    }

    @Test
    void uiCaptureUsesRegisteredOffscreenProviderWhenAutoSelected() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("offscreen-test", 500));
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "source", "auto"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("offscreen", ((Map<?, ?>) payload.get("imageMeta")).get("source"));
        assertEquals("offscreen-test", ((Map<?, ?>) payload.get("imageMeta")).get("providerId"));
    }

    @Test
    void uiCapturePrefersOffscreenOverFramebufferWhenBothMatchAuto() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiFramebufferCaptureProvider(new TestFramebufferCaptureProvider("framebuffer-test", 400));
        mod.api().registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("offscreen-test", 500));
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "source", "auto"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("offscreen", ((Map<?, ?>) payload.get("imageMeta")).get("source"));
        assertEquals("offscreen-test", ((Map<?, ?>) payload.get("imageMeta")).get("providerId"));
    }

    @Test
    void uiCaptureUsesFramebufferProviderWhenExplicitlyRequested() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("offscreen-test", 500));
        mod.api().registerUiFramebufferCaptureProvider(new TestFramebufferCaptureProvider("framebuffer-test", 400));
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "source", "framebuffer"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("framebuffer", ((Map<?, ?>) payload.get("imageMeta")).get("source"));
        assertEquals("framebuffer-test", ((Map<?, ?>) payload.get("imageMeta")).get("providerId"));
    }

    @Test
    void uiActionDelegatesTargetAndActionToDriver() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "action", "click",
                "target", Map.of("role", "slot")
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("vanilla-container", payload.get("driverId"));
        assertEquals("click", payload.get("action"));
        assertTrue(payload.containsKey("preSnapshotRef"));
        assertTrue(payload.containsKey("postSnapshotRef"));
        assertEquals("vanilla-container", ((Map<?, ?>) payload.get("postActionSnapshot")).get("driverId"));
    }

    @Test
    void uiQueryRejectsTargetsWhenScopeOrModIdDoNotMatch() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_query").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "modId", "minecraft",
                "selector", Map.of(
                        "scope", "screen",
                        "id", "slot-0",
                        "modId", "other-mod"
                )
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(0, ((List<?>) payload.get("targets")).size());
    }

    @Test
    void uiQueryRejectsTargetsWhenBoundsDoNotIntersect() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_query").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "selector", Map.of(
                        "text", "Viewport",
                        "bounds", Map.of(
                                "x", 500,
                                "y", 500,
                                "width", 20,
                                "height", 20
                        )
                )
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("fallback-region", payload.get("driverId"));
        assertEquals(0, ((List<?>) payload.get("targets")).size());
    }

    @Test
    void uiInspectAtReturnsTopmostTargetUnderPoint() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_inspect_at").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "x", 10,
                "y", 20
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("vanilla-container", payload.get("driverId"));
        assertEquals("slot-0", ((Map<?, ?>) payload.get("topmostTarget")).get("targetId"));
        assertEquals(2, ((List<?>) payload.get("targets")).size());
    }

    @Test
    void uiGetTargetDetailsReturnsMatchedTargetDetails() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_get_target_details").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "target", Map.of("id", "slot-0")
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("vanilla-container", payload.get("driverId"));
        assertEquals("slot-0", ((Map<?, ?>) payload.get("target")).get("targetId"));
        assertEquals("slot", ((Map<?, ?>) payload.get("target")).get("role"));
        assertEquals("slot-0", ((Map<?, ?>) payload.get("captureRegion")).get("targetId"));
    }

    @Test
    void uiCaptureSupportsMultipleTargets() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "mode", "composite",
                "targets", List.of(
                        Map.of("id", "container-root"),
                        Map.of("id", "slot-0")
                )
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("vanilla-container", payload.get("driverId"));
        assertEquals("composite", payload.get("mode"));
        assertEquals(2, ((List<?>) payload.get("capturedTargets")).size());
    }

    @Test
    void uiCaptureSupportsExcludingMatchedTargets() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "mode", "composite",
                "targets", List.of(
                        Map.of("id", "container-root"),
                        Map.of("id", "slot-0")
                ),
                "exclude", List.of(
                        Map.of("id", "slot-0")
                )
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(1, ((List<?>) payload.get("capturedTargets")).size());
        assertEquals(1, ((List<?>) payload.get("excludedTargets")).size());
        assertEquals("slot-0", ((Map<?, ?>) ((List<?>) payload.get("excludedTargets")).getFirst()).get("targetId"));
    }

    @Test
    void uiWaitReturnsMatchedTargetsForPresentSelector() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "selector", Map.of("id", "slot-0")
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("vanilla-container", payload.get("driverId"));
        assertEquals(true, payload.get("matched"));
        assertEquals(1, ((List<?>) payload.get("targets")).size());
    }

    @Test
    void uiWaitReturnsUnmatchedForMissingSelector() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "selector", Map.of("id", "missing-target")
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(false, payload.get("matched"));
        assertEquals(0, ((List<?>) payload.get("targets")).size());
    }

    @Test
    void uiGetTooltipReturnsLinesForMatchedTarget() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_get_tooltip").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "target", Map.of("id", "slot-0")
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("vanilla-container", payload.get("driverId"));
        assertEquals("slot-0", payload.get("targetId"));
        assertEquals(List.of("Slot 0"), payload.get("lines"));
    }

    @Test
    void uiOpenDelegatesSemanticOpenAction() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_open").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "target", "inventory"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("fallback-region", payload.get("driverId"));
        assertEquals("open", payload.get("action"));
        assertEquals(true, payload.get("performed"));
        assertTrue(payload.containsKey("postSnapshotRef"));
        assertEquals("fallback-region", ((Map<?, ?>) payload.get("postActionSnapshot")).get("driverId"));
    }

    @Test
    void uiCloseDelegatesSemanticCloseAction() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_close").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "target", "screen"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("fallback-region", payload.get("driverId"));
        assertEquals("close", payload.get("action"));
        assertTrue(payload.containsKey("postSnapshotRef"));
        assertEquals(List.of(), ((Map<?, ?>) payload.get("postActionSnapshot")).get("targets"));
    }

    @Test
    void uiSwitchDelegatesSemanticSwitchAction() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_switch").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "target", Map.of("id", "slot-0")
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("vanilla-container", payload.get("driverId"));
        assertEquals("switch", payload.get("action"));
        assertTrue(payload.containsKey("preSnapshotRef"));
        assertTrue(payload.containsKey("postSnapshotRef"));
        assertEquals("slot-0", ((Map<?, ?>) payload.get("postActionSnapshot")).get("focusedTargetId"));
    }

    @Test
    void uiSwitchUpdatesSubsequentSnapshotFocusState() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var switchTool = server.registry().findTool("moddev.ui_switch").orElseThrow();
        switchTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "target", Map.of("id", "slot-0")
        ));

        var snapshotTool = server.registry().findTool("moddev.ui_snapshot").orElseThrow();
        var snapshotResult = snapshotTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) snapshotResult.value();
        assertEquals("slot-0", payload.get("focusedTargetId"));
    }

    @Test
    void uiCloseClearsAndUiOpenRestoresSubsequentInteractionState() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var closeTool = server.registry().findTool("moddev.ui_close").orElseThrow();
        closeTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen"
        ));

        var stateTool = server.registry().findTool("moddev.ui_get_interaction_state").orElseThrow();
        var closedStateResult = stateTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen"
        ));

        @SuppressWarnings("unchecked")
        var closedPayload = (Map<String, Object>) closedStateResult.value();
        assertEquals(Map.of(), closedPayload.get("focusedTarget"));
        assertEquals("closed", closedPayload.get("selectionSource"));

        var openTool = server.registry().findTool("moddev.ui_open").orElseThrow();
        openTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen"
        ));

        var openedStateResult = stateTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen"
        ));

        @SuppressWarnings("unchecked")
        var openedPayload = (Map<String, Object>) openedStateResult.value();
        assertEquals("container-root", ((Map<?, ?>) openedPayload.get("focusedTarget")).get("targetId"));
        assertEquals("programmatic", openedPayload.get("selectionSource"));
    }

    @Test
    void customInteractionResolverCanOverrideBuiltinDefaultFocus() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiInteractionStateResolver(new TestUiInteractionStateResolver());
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_snapshot").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("slot-0", payload.get("focusedTargetId"));
    }

    private static final class TestUiInteractionStateResolver implements UiInteractionStateResolver {

        @Override
        public String driverId() {
            return "vanilla-container";
        }

        @Override
        public int priority() {
            return 10_000;
        }

        @Override
        public boolean matches(UiContext context, List<UiTarget> targets) {
            return true;
        }

        @Override
        public UiInteractionDefaults resolve(UiContext context, List<UiTarget> targets) {
            return new UiInteractionDefaults("slot-0", "slot-0", "slot-0", "slot-0", "test-resolver");
        }
    }

    private static final class TestOffscreenCaptureProvider implements UiOffscreenCaptureProvider {

        private final String providerId;
        private final int priority;

        private TestOffscreenCaptureProvider(String providerId, int priority) {
            this.providerId = providerId;
            this.priority = priority;
        }

        @Override
        public String providerId() {
            return providerId;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public boolean matches(UiContext context, UiSnapshot snapshot) {
            return true;
        }

        @Override
        public UiCaptureImage capture(UiContext context, UiSnapshot snapshot, CaptureRequest request, List<UiTarget> capturedTargets, List<UiTarget> excludedTargets) {
            var bytes = new UiCaptureRenderer().render(snapshot, capturedTargets, excludedTargets);
            return new UiCaptureImage(providerId, "offscreen", bytes, 320, 240, Map.of());
        }
    }

    private static final class TestFramebufferCaptureProvider implements UiFramebufferCaptureProvider {

        private final String providerId;
        private final int priority;

        private TestFramebufferCaptureProvider(String providerId, int priority) {
            this.providerId = providerId;
            this.priority = priority;
        }

        @Override
        public String providerId() {
            return providerId;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public boolean matches(UiContext context, UiSnapshot snapshot) {
            return true;
        }

        @Override
        public UiCaptureImage capture(UiContext context, UiSnapshot snapshot, CaptureRequest request, List<UiTarget> capturedTargets, List<UiTarget> excludedTargets) {
            var bytes = new UiCaptureRenderer().render(snapshot, capturedTargets, excludedTargets);
            return new UiCaptureImage(providerId, "framebuffer", bytes, 320, 240, Map.of());
        }
    }
}
