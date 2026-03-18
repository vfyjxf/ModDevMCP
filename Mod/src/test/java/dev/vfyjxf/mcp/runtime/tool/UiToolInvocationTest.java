package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.api.ui.*;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.runtime.ui.FallbackRegionUiDriver;
import dev.vfyjxf.mcp.runtime.ui.UiCaptureRenderer;
import dev.vfyjxf.mcp.runtime.ui.VanillaScreenUiDriver;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

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
    void uiInspectReturnsConciseInspectPayload() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new InspectActionUiDriver("custom.InspectScreen"));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.InspectScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_inspect").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of());

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("custom.InspectScreen", payload.get("screen"));
        assertEquals("inspect-action-driver", payload.get("driverId"));
        assertTrue(payload.containsKey("summary"));
        assertTrue(payload.containsKey("targets"));
        assertTrue(payload.containsKey("interaction"));
        assertFalse(payload.containsKey("snapshotRef"));
        assertFalse(payload.containsKey("focusedTargetId"));
    }

    @Test
    void uiActResolvesLocatorAndPerformsAction() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var driver = new InspectActionUiDriver("custom.InspectScreen");
        registries.uiDrivers().register(driver);
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.InspectScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_act").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "click",
                "locator", Map.of("role", "button", "text", "Launch")
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("inspect-action-driver", payload.get("driverId"));
        assertEquals("click", payload.get("action"));
        assertEquals("launch-button", ((Map<?, ?>) payload.get("resolvedTarget")).get("targetId"));
        assertEquals("launch-button", driver.lastActionTargetId.get());
    }

    @Test
    void uiActAcceptsRefShortcut() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var driver = new InspectActionUiDriver("custom.InspectScreen");
        registries.uiDrivers().register(driver);
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.InspectScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_act").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "click",
                "ref", "launch-button"
        ));

        assertTrue(result.success());
        assertEquals("launch-button", driver.lastActionTargetId.get());
    }

    @Test
    void uiActReturnsStableActionabilityErrorForDisabledTarget() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new InspectActionUiDriver("custom.InspectScreen"));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.InspectScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_act").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "click",
                "locator", Map.of("role", "button", "text", "Disabled")
        ));

        assertFalse(result.success());
        assertEquals("target_disabled", result.error());
    }

    @Test
    void uiSessionOpenReturnsSessionIdAndRefs() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertFalse(((String) payload.get("sessionId")).isBlank());
        assertEquals("vanilla-container", payload.get("driverId"));
        assertTrue(((List<?>) payload.get("refs")).size() >= 1);
    }

    @Test
    void uiSessionRefreshReportsScreenChangeFromLiveProbe() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var probe = new MutableClientScreenProbe(new ClientScreenMetrics(
                "custom.RejectScreen",
                320,
                240,
                854,
                480
        ));
        registries.uiDrivers().register(new RejectingUiDriver("custom.RejectScreen"));
        registries.uiDrivers().register(new RejectingUiDriver("custom.NextScreen"));
        new UiToolProvider(registries, probe).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());

        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        var sessionId = (String) openPayload.get("sessionId");

        probe.metrics.set(new ClientScreenMetrics(
                "custom.NextScreen",
                320,
                240,
                854,
                480
        ));

        var refreshTool = server.registry().findTool("moddev.ui_session_refresh").orElseThrow();
        var refreshResult = refreshTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", sessionId
        ));

        @SuppressWarnings("unchecked")
        var refreshPayload = (Map<String, Object>) refreshResult.value();
        assertEquals(true, refreshPayload.get("screenChanged"));
        assertEquals("custom.NextScreen", refreshPayload.get("screenClass"));
    }

    @Test
    void uiSessionRefreshReturnsSessionNotFoundForUnknownSession() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RejectScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_session_refresh").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", "missing-session"
        ));

        assertFalse(result.success());
        assertEquals("session_not_found", result.error());
    }

    @Test
    void uiSessionRefreshReturnsScreenUnavailableWhenLiveScreenDisappears() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var probe = new MutableClientScreenProbe(new ClientScreenMetrics(
                "custom.RejectScreen",
                320,
                240,
                854,
                480
        ));
        registries.uiDrivers().register(new RejectingUiDriver("custom.RejectScreen"));
        new UiToolProvider(registries, probe).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());

        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        probe.metrics.set(new ClientScreenMetrics(null, 0, 0, 0, 0));

        var refreshTool = server.registry().findTool("moddev.ui_session_refresh").orElseThrow();
        var refreshResult = refreshTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId")
        ));

        assertFalse(refreshResult.success());
        assertEquals("screen_unavailable", refreshResult.error());
    }

    @Test
    void uiSessionOpenReturnsRuntimeUnavailableWhenProbeFails() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        new UiToolProvider(registries, () -> {
            throw new IllegalStateException("probe offline");
        }).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of());

        assertFalse(result.success());
        assertEquals("runtime_unavailable", result.error());
    }

    @Test
    void uiClickRefResolvesSessionRefAndPerformsClick() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var driver = new RecordingRefUiDriver("custom.RefScreen");
        registries.uiDrivers().register(driver);
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RefScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());
        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        @SuppressWarnings("unchecked")
        var firstRef = (Map<String, Object>) ((List<?>) openPayload.get("refs")).getFirst();

        var clickTool = server.registry().findTool("moddev.ui_click_ref").orElseThrow();
        var clickResult = clickTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId"),
                "refId", firstRef.get("refId")
        ));

        assertTrue(clickResult.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) clickResult.value();
        assertEquals("click", payload.get("action"));
        assertEquals("ref-target", driver.lastActionTargetId.get());
    }

    @Test
    void uiHoverRefResolvesSessionRefAndPerformsHover() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var driver = new RecordingRefUiDriver("custom.RefScreen");
        registries.uiDrivers().register(driver);
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RefScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());
        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        @SuppressWarnings("unchecked")
        var firstRef = (Map<String, Object>) ((List<?>) openPayload.get("refs")).getFirst();

        var hoverTool = server.registry().findTool("moddev.ui_hover_ref").orElseThrow();
        var hoverResult = hoverTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId"),
                "refId", firstRef.get("refId"),
                "hoverDelayMs", 75
        ));

        assertTrue(hoverResult.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) hoverResult.value();
        assertEquals("hover", payload.get("action"));
        assertEquals("hover", driver.lastAction.get());
    }

    @Test
    void uiClickRefUsesSessionContextEvenWhenCallerPassesDifferentScreen() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var driver = new RecordingRefUiDriver("custom.RefScreen");
        registries.uiDrivers().register(driver);
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RefScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());
        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        @SuppressWarnings("unchecked")
        var firstRef = (Map<String, Object>) ((List<?>) openPayload.get("refs")).getFirst();

        var clickTool = server.registry().findTool("moddev.ui_click_ref").orElseThrow();
        var clickResult = clickTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId"),
                "refId", firstRef.get("refId"),
                "screenClass", "custom.WrongScreen",
                "modId", "wrong-mod"
        ));

        assertTrue(clickResult.success());
        assertEquals("custom.RefScreen", driver.lastContextScreenClass.get());
        assertEquals("minecraft", driver.lastContextModId.get());
    }

    @Test
    void uiClickRefRefreshesSessionOnDemandBeforeResolvingStaleTarget() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var probe = new MutableClientScreenProbe(new ClientScreenMetrics(
                "custom.RefScreen",
                320,
                240,
                854,
                480
        ));
        var currentDriver = new RecordingRefUiDriver("custom.RefScreen", "ref-target");
        var nextDriver = new RecordingRefUiDriver("custom.NextScreen", "next-target");
        registries.uiDrivers().register(currentDriver);
        registries.uiDrivers().register(nextDriver);
        new UiToolProvider(registries, probe).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());
        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        @SuppressWarnings("unchecked")
        var firstRef = (Map<String, Object>) ((List<?>) openPayload.get("refs")).getFirst();

        probe.metrics.set(new ClientScreenMetrics(
                "custom.NextScreen",
                320,
                240,
                854,
                480
        ));

        var clickTool = server.registry().findTool("moddev.ui_click_ref").orElseThrow();
        var clickResult = clickTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId"),
                "refId", firstRef.get("refId")
        ));

        assertFalse(clickResult.success());
        assertEquals("target_stale", clickResult.error());
        assertEquals("custom.NextScreen", registries.uiAutomationSessions()
                .find((String) openPayload.get("sessionId"))
                .orElseThrow()
                .snapshot()
                .screenClass());
        assertEquals(0, currentDriver.actionCount.get());
        assertEquals(0, nextDriver.actionCount.get());
    }

    @Test
    void uiClickRefReturnsSessionStaleWhenLiveScreenCannotBeRefreshed() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var probe = new MutableClientScreenProbe(new ClientScreenMetrics(
                "custom.RefScreen",
                320,
                240,
                854,
                480
        ));
        var driver = new RecordingRefUiDriver("custom.RefScreen", "ref-target");
        registries.uiDrivers().register(driver);
        new UiToolProvider(registries, probe).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());
        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        @SuppressWarnings("unchecked")
        var firstRef = (Map<String, Object>) ((List<?>) openPayload.get("refs")).getFirst();

        probe.metrics.set(new ClientScreenMetrics(
                "custom.UnsupportedScreen",
                320,
                240,
                854,
                480
        ));

        var clickTool = server.registry().findTool("moddev.ui_click_ref").orElseThrow();
        var clickResult = clickTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId"),
                "refId", firstRef.get("refId")
        ));

        assertFalse(clickResult.success());
        assertEquals("session_stale", clickResult.error());
        assertEquals(0, driver.actionCount.get());
    }

    @Test
    void uiClickRefRefreshesSameScreenClassInstanceBeforeReturningTargetStale() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var probe = new MutableClientScreenProbe(new ClientScreenMetrics(
                "custom.RefScreen",
                320,
                240,
                854,
                480
        ));
        var driver = new MutableRefUiDriver("custom.RefScreen", "screen-1", "ref-target");
        registries.uiDrivers().register(driver);
        new UiToolProvider(registries, probe).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());
        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        @SuppressWarnings("unchecked")
        var firstRef = (Map<String, Object>) ((List<?>) openPayload.get("refs")).getFirst();

        driver.screenId.set("screen-2");
        driver.targetId.set("next-target");

        var clickTool = server.registry().findTool("moddev.ui_click_ref").orElseThrow();
        var clickResult = clickTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId"),
                "refId", firstRef.get("refId")
        ));

        assertFalse(clickResult.success());
        assertEquals("target_stale", clickResult.error());
        assertEquals("screen-2", registries.uiAutomationSessions()
                .find((String) openPayload.get("sessionId"))
                .orElseThrow()
                .snapshot()
                .screenId());
        assertEquals(0, driver.actionCount.get());
    }

    @Test
    void uiPressKeyDelegatesToInputController() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var controller = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(controller);
        new UiToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_press_key").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "keyCode", 69,
                "scanCode", 18,
                "modifiers", 0
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("key_press", payload.get("action"));
        assertEquals("key_press", controller.lastAction.get());
    }

    @Test
    void uiTypeTextDelegatesToInputController() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var controller = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(controller);
        new UiToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_type_text").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "text", "hello"
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("type_text", payload.get("action"));
        assertEquals("type_text", controller.lastAction.get());
    }

    @Test
    void uiWaitForUsesSessionRefToWaitForTarget() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var driver = new RecordingRefUiDriver("custom.RefScreen");
        registries.uiDrivers().register(driver);
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RefScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());
        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        @SuppressWarnings("unchecked")
        var firstRef = (Map<String, Object>) ((List<?>) openPayload.get("refs")).getFirst();

        var waitTool = server.registry().findTool("moddev.ui_wait_for").orElseThrow();
        var waitResult = waitTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId"),
                "refId", firstRef.get("refId"),
                "condition", "appeared",
                "timeoutMs", 50
        ));

        assertTrue(waitResult.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) waitResult.value();
        assertEquals(true, payload.get("matched"));
    }

    @Test
    void uiWaitForReturnsSessionNotFoundForUnknownSession() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RefScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_wait_for").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", "missing-session",
                "condition", "appeared"
        ));

        assertFalse(result.success());
        assertEquals("session_not_found", result.error());
    }

    @Test
    void uiScreenshotUsesCaptureFlowForSessionRef() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        var driver = new RecordingRefUiDriver("custom.RefScreen");
        registries.uiDrivers().register(driver);
        mod.api().registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("offscreen-test", 500));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RefScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());
        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();
        @SuppressWarnings("unchecked")
        var firstRef = (Map<String, Object>) ((List<?>) openPayload.get("refs")).getFirst();

        var screenshotTool = server.registry().findTool("moddev.ui_screenshot").orElseThrow();
        var screenshotResult = screenshotTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId"),
                "refId", firstRef.get("refId")
        ));

        assertTrue(screenshotResult.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) screenshotResult.value();
        assertEquals("offscreen", ((Map<?, ?>) payload.get("imageMeta")).get("source"));
    }

    @Test
    void uiScreenshotReturnsSessionNotFoundForUnknownSession() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RefScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_screenshot").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", "missing-session"
        ));

        assertFalse(result.success());
        assertEquals("session_not_found", result.error());
    }

    @Test
    void uiScreenshotSupportsHighLevelLiveScreenFlowWithConcisePayload() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        registries.uiDrivers().register(new InspectActionUiDriver("custom.InspectScreen"));
        mod.api().registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("offscreen-test", 500));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.InspectScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_screenshot").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "locator", Map.of("role", "button", "text", "Launch"),
                "source", "auto"
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("inspect-action-driver", payload.get("driverId"));
        assertEquals("launch-button", ((Map<?, ?>) payload.get("resolvedTarget")).get("targetId"));
        assertEquals("offscreen", ((Map<?, ?>) payload.get("imageMeta")).get("source"));
        assertTrue(payload.containsKey("snapshotRef"));
        assertTrue(payload.containsKey("imageRef"));
        assertFalse(payload.containsKey("capturedSnapshot"));
        assertFalse(payload.containsKey("capturedTargets"));
        assertFalse(payload.containsKey("excludedTargets"));
    }

    @Test
    void uiClickRefReturnsSessionNotFoundBeforeScreenAvailabilityChecks() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics(null, 0, 0, 0, 0)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_click_ref").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", "missing-session",
                "refId", "ref-1"
        ));

        assertFalse(result.success());
        assertEquals("session_not_found", result.error());
    }

    @Test
    void uiClickRefReturnsSessionNotFoundForUnknownSession() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RefScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_click_ref").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", "missing-session",
                "refId", "ref-1"
        ));

        assertFalse(result.success());
        assertEquals("session_not_found", result.error());
    }

    @Test
    void uiClickRefReturnsTargetNotFoundForUnknownRef() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var driver = new RecordingRefUiDriver("custom.RefScreen");
        registries.uiDrivers().register(driver);
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RefScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var openTool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
        var openResult = openTool.handler().handle(ToolCallContext.empty(), Map.of());
        @SuppressWarnings("unchecked")
        var openPayload = (Map<String, Object>) openResult.value();

        var clickTool = server.registry().findTool("moddev.ui_click_ref").orElseThrow();
        var clickResult = clickTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", openPayload.get("sessionId"),
                "refId", "missing-ref"
        ));

        assertFalse(clickResult.success());
        assertEquals("target_not_found", clickResult.error());
    }

    @Test
    void uiSnapshotUsesLiveScreenWhenScreenClassIsOmitted() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new RejectingUiDriver("custom.RejectScreen"));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RejectScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_snapshot").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of());

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("rejecting-test", payload.get("driverId"));
        assertEquals("custom.RejectScreen", payload.get("screenClass"));
    }

    @Test
    void uiSnapshotDoesNotReadLiveScreenWhenScreenClassIsExplicit() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new RejectingUiDriver("custom.RejectScreen"));
        new UiToolProvider(registries, () -> {
            throw new AssertionError("screenProbe should not be used when screenClass is explicit");
        }).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_snapshot").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.RejectScreen",
                "modId", "minecraft"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("rejecting-test", payload.get("driverId"));
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
    void uiQueryAggregatesAcrossFilteredDrivers() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new CompositeTestUiDriver(
                "base",
                100,
                "custom.CompositeScreen",
                List.of(buttonTarget("base", "play", "Play"))
        ));
        registries.uiDrivers().register(new CompositeTestUiDriver(
                "addon",
                300,
                "custom.CompositeScreen",
                List.of(buttonTarget("addon", "pin", "Pin"))
        ));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.CompositeScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_query").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "selector", Map.of("role", "button"),
                "includeDrivers", List.of("base", "addon")
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("addon", payload.get("driverId"));
        assertEquals(
                Set.of("base:play", "addon:pin"),
                targetKeys((List<Map<String, Object>>) payload.get("targets"))
        );
        assertEquals(
                List.of("addon", "base"),
                driverIds((List<Map<String, Object>>) payload.get("drivers"))
        );
    }

    @Test
    void uiSnapshotCanExcludeSpecificDrivers() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new CompositeTestUiDriver(
                "base",
                100,
                "custom.CompositeScreen",
                List.of(buttonTarget("base", "root", "Base Root"))
        ));
        registries.uiDrivers().register(new CompositeTestUiDriver(
                "addon",
                300,
                "custom.CompositeScreen",
                List.of(buttonTarget("addon", "pin", "Pin"))
        ));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.CompositeScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_snapshot").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "excludeDrivers", List.of("addon")
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("base", payload.get("driverId"));
        assertEquals(
                List.of("base"),
                driverIds((List<Map<String, Object>>) payload.get("drivers"))
        );
        assertEquals(
                Set.of("base:root"),
                targetKeys((List<Map<String, Object>>) payload.get("targets"))
        );
    }

    @Test
    void uiCaptureUsesLiveScreenWhenScreenClassIsOmitted() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.prepareClientRuntime();
        mod.api().registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("offscreen-test", 500));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "source", "auto"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("vanilla-screen", payload.get("driverId"));
        assertEquals("offscreen", ((Map<?, ?>) payload.get("imageMeta")).get("source"));
        assertEquals("offscreen-test", ((Map<?, ?>) payload.get("imageMeta")).get("providerId"));
    }

    @Test
    void uiCaptureUsesTrackedPointerWhenMouseCoordinatesAreOmitted() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var provider = new RecordingPointerOffscreenCaptureProvider("offscreen-test", 500);
        registries.uiPointerStates().update("net.minecraft.client.gui.screens.TitleScreen", "minecraft", 200, 100);
        var mod = new ModDevMCP(server, registries);
        mod.prepareClientRuntime();
        mod.api().registerUiOffscreenCaptureProvider(provider);
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "source", "auto"
        ));

        assertTrue(result.success());
        assertEquals(200, provider.lastMouseX.get());
        assertEquals(100, provider.lastMouseY.get());
    }

    @Test
    void uiGetInteractionStateUsesTrackedPointerWhenMouseCoordinatesAreOmitted() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiPointerStates().update("net.minecraft.client.gui.screens.TitleScreen", "minecraft", 200, 100);
        var mod = new ModDevMCP(server, registries);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_get_interaction_state").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.TitleScreen"
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(200, payload.get("cursorX"));
        assertEquals(100, payload.get("cursorY"));
    }

    @Test
    void uiCaptureFailsWhenOnlyPlaceholderCaptureWouldBeAvailable() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "mode", "crop"
        ));

        assertFalse(result.success());
        assertEquals("capture_unavailable: no real UI capture provider matched source=auto, driver=fallback-region, screenClass=custom.UnknownScreen", result.error());
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
    void uiCaptureReturnsFailureWhenExplicitTargetSelectorMatchesNothing() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "target", Map.of("id", "missing-target")
        ));

        assertFalse(result.success());
        assertEquals("target_not_found: selector did not match any target", result.error());
    }

    @Test
    void uiCaptureReturnsFailureWhenAllExplicitTargetsMiss() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "targets", List.of(
                        Map.of("id", "missing-a"),
                        Map.of("id", "missing-b")
                )
        ));

        assertFalse(result.success());
        assertEquals("target_not_found: selector did not match any target", result.error());
    }

    @Test
    void uiCaptureCropsStoredArtifactToExplicitTargetBounds() throws Exception {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiDriver(new InspectActionUiDriver("custom.InspectScreen"));
        mod.api().registerUiOffscreenCaptureProvider(new FixedImageOffscreenCaptureProvider(
                "offscreen-test",
                500,
                100,
                100,
                Color.WHITE
        ));
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.InspectScreen",
                "source", "auto",
                "mode", "crop",
                "target", Map.of("id", "launch-button")
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        @SuppressWarnings("unchecked")
        var imageMeta = (Map<String, Object>) payload.get("imageMeta");
        assertEquals(40, ((Number) imageMeta.get("width")).intValue());
        assertEquals(20, ((Number) imageMeta.get("height")).intValue());

        var image = ImageIO.read(Path.of((String) payload.get("imagePath")).toFile());
        assertEquals(40, image.getWidth());
        assertEquals(20, image.getHeight());
    }

    @Test
    void uiCaptureMasksExcludedTargetsInStoredArtifact() throws Exception {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiDriver(new InspectActionUiDriver("custom.InspectScreen"));
        mod.api().registerUiOffscreenCaptureProvider(new FixedImageOffscreenCaptureProvider(
                "offscreen-test",
                500,
                100,
                100,
                Color.WHITE
        ));
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.InspectScreen",
                "source", "auto",
                "mode", "full",
                "exclude", List.of(Map.of("id", "disabled-button"))
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();

        var image = ImageIO.read(Path.of((String) payload.get("imagePath")).toFile());
        var includedPixel = new Color(image.getRGB(15, 25), true);
        var excludedPixel = new Color(image.getRGB(65, 25), true);

        assertEquals(255, includedPixel.getRed());
        assertEquals(255, includedPixel.getGreen());
        assertEquals(255, includedPixel.getBlue());
        assertEquals(0, excludedPixel.getRed());
        assertEquals(0, excludedPixel.getGreen());
        assertEquals(0, excludedPixel.getBlue());
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
    void uiActionFailsWhenMatchingTargetsSpanMultipleDriversWithoutExplicitDriver() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new CompositeTestUiDriver(
                "base",
                100,
                "custom.CompositeScreen",
                List.of(buttonTarget("base", "shared-id", "Shared"))
        ));
        registries.uiDrivers().register(new CompositeTestUiDriver(
                "addon",
                300,
                "custom.CompositeScreen",
                List.of(buttonTarget("addon", "shared-id", "Shared"))
        ));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.CompositeScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "click",
                "target", Map.of("id", "shared-id")
        ));

        assertFalse(result.success());
        assertEquals("target_ambiguous", result.error());
    }

    @Test
    void uiActionUsesLiveScreenWhenScreenClassIsOmitted() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new RejectingUiDriver("custom.RejectScreen"));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.RejectScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "click",
                "target", Map.of("id", "reject-target")
        ));

        assertFalse(result.success());
        assertEquals("unsupported: action rejected by driver rejecting-test: Action disabled for test", result.error());
    }

    @Test
    void uiActionCanWaitForPostActionUiCondition() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiDriver(new WaitTestUiDriver("custom.WaitScreen", true));
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.WaitScreen",
                "action", "type",
                "target", Map.of("id", "wait-target"),
                "text", "Updated Target",
                "waitCondition", "text_changed",
                "waitTimeoutMs", 500,
                "waitPollIntervalMs", 10
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("wait-test", payload.get("driverId"));
        assertEquals("type", payload.get("action"));
        assertEquals(true, ((Map<?, ?>) payload.get("wait")).get("matched"));
        assertEquals(false, ((Map<?, ?>) payload.get("wait")).get("timedOut"));
        assertEquals(1, ((List<?>) ((Map<?, ?>) payload.get("wait")).get("targets")).size());
    }

    @Test
    void uiActionReturnsFailureWhenTargetSelectorMatchesNothing() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "action", "click",
                "target", Map.of("id", "missing-target")
        ));

        assertFalse(result.success());
        assertEquals("target_not_found: selector did not match any target", result.error());
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
    void uiQueryReturnsFailureWhenNoDriverMatches() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        new UiToolProvider(new RuntimeRegistries()).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_query").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.NoDriverScreen",
                "modId", "test-mod"
        ));

        assertFalse(result.success());
        assertEquals("unsupported: no ui driver matched screenClass=custom.NoDriverScreen, modId=test-mod", result.error());
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
        assertEquals("minecraft", payload.get("modId"));
        assertEquals("slot", payload.get("role"));
        assertEquals("", payload.get("text"));
        assertEquals(Map.of("x", 8, "y", 18, "width", 16, "height", 16), payload.get("bounds"));
        assertEquals(Map.of("slotIndex", 0), payload.get("extensions"));
        assertEquals(2, ((List<?>) payload.get("targets")).size());
    }

    @Test
    void uiInspectAtReturnsFailureWhenDriverRejectsInspect() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new RejectingUiDriver("custom.RejectScreen"));
        new UiToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_inspect_at").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.RejectScreen",
                "x", 4,
                "y", 8
        ));

        assertFalse(result.success());
        assertEquals("unsupported: inspect rejected by driver rejecting-test: Inspect disabled for test", result.error());
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
        assertEquals(List.of("container-root", "slot-0"), payload.get("hierarchyPath"));
        assertEquals(false, ((Map<?, ?>) payload.get("interactionState")).get("focused"));
        assertEquals(List.of("click", "hover"), payload.get("actions"));
        assertEquals(false, payload.get("overlay"));
        assertEquals(Map.of("slotIndex", 0), payload.get("metadata"));
        assertEquals(Map.of("slotIndex", 0), payload.get("extensions"));
    }

    @Test
    void uiGetTargetDetailsAcceptsTargetTargetId() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_get_target_details").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "target", Map.of("targetId", "slot-0")
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("slot-0", ((Map<?, ?>) payload.get("target")).get("targetId"));
        assertEquals(List.of("container-root", "slot-0"), payload.get("hierarchyPath"));
    }

    @Test
    void uiGetTargetDetailsReturnsFailureWhenTargetIsMissing() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_get_target_details").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "target", Map.of("id", "missing-target")
        ));

        assertFalse(result.success());
        assertEquals("target_not_found: selector did not match any target", result.error());
    }

    @Test
    void uiCaptureSupportsMultipleTargets() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("offscreen-test", 500));
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
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("offscreen-test", 500));
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
    void uiCaptureReturnsFailureWhenExcludeRemovesAllExplicitTargets() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_capture").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "mode", "composite",
                "targets", List.of(
                        Map.of("id", "slot-0")
                ),
                "exclude", List.of(
                        Map.of("id", "slot-0")
                )
        ));

        assertFalse(result.success());
        assertEquals("target_not_found: explicit targets were excluded or resolved to no capturable targets", result.error());
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
    void uiWaitUsesDriverWaitForAndReturnsStableErrorCode() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new DriverWaitUiDriver("custom.WaitApiScreen"));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.WaitApiScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "condition", "targetAppeared",
                "locator", Map.of("role", "button", "text", "Wait Target"),
                "timeoutMs", 75,
                "pollIntervalMs", 10
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("driver-wait", payload.get("driverId"));
        assertEquals(false, payload.get("matched"));
        assertEquals("timeout", payload.get("errorCode"));
        assertEquals("wait-target", ((Map<?, ?>) payload.get("matchedTarget")).get("targetId"));
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
    void uiWaitPollsUntilSelectorAppearsWithinTimeout() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        var driver = new WaitTestUiDriver("custom.WaitScreen", false);
        mod.api().registerUiDriver(driver);
        mod.registerBuiltinProviders();

        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.schedule(() -> driver.visible.set(true), 50, TimeUnit.MILLISECONDS);

            var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
            var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                    "screenClass", "custom.WaitScreen",
                    "selector", Map.of("id", "wait-target"),
                    "condition", "appeared",
                    "timeoutMs", 500,
                    "pollIntervalMs", 10
            ));

            @SuppressWarnings("unchecked")
            var payload = (Map<String, Object>) result.value();
            assertEquals("wait-test", payload.get("driverId"));
            assertEquals(true, payload.get("matched"));
            assertEquals(false, payload.get("timedOut"));
            assertEquals(1, ((List<?>) payload.get("targets")).size());
            assertTrue(((Number) payload.get("elapsedMs")).longValue() >= 0);
        }
    }

    @Test
    void uiWaitPollsUntilSelectorDisappearsWithinTimeout() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        var driver = new WaitTestUiDriver("custom.WaitScreen", true);
        mod.api().registerUiDriver(driver);
        mod.registerBuiltinProviders();

        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.schedule(() -> driver.visible.set(false), 50, TimeUnit.MILLISECONDS);

            var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
            var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                    "screenClass", "custom.WaitScreen",
                    "selector", Map.of("id", "wait-target"),
                    "condition", "disappeared",
                    "timeoutMs", 500,
                    "pollIntervalMs", 10
            ));

            @SuppressWarnings("unchecked")
            var payload = (Map<String, Object>) result.value();
            assertEquals("wait-test", payload.get("driverId"));
            assertEquals(true, payload.get("matched"));
            assertEquals(false, payload.get("timedOut"));
            assertEquals(0, ((List<?>) payload.get("targets")).size());
            assertTrue(((Number) payload.get("elapsedMs")).longValue() >= 0);
        }
    }

    @Test
    void uiWaitReportsTimeoutWhenSelectorDoesNotAppear() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiDriver(new WaitTestUiDriver("custom.WaitScreen", false));
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.WaitScreen",
                "selector", Map.of("id", "wait-target"),
                "condition", "appeared",
                "timeoutMs", 80,
                "pollIntervalMs", 10
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("wait-test", payload.get("driverId"));
        assertEquals(false, payload.get("matched"));
        assertEquals(true, payload.get("timedOut"));
        assertEquals(0, ((List<?>) payload.get("targets")).size());
        assertTrue(((Number) payload.get("elapsedMs")).longValue() >= 80);
    }

    @Test
    void uiWaitPollsUntilScreenChangesWithinTimeout() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        var driver = new WaitTestUiDriver("custom.WaitScreen", true);
        mod.api().registerUiDriver(driver);
        mod.registerBuiltinProviders();

        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.schedule(() -> driver.screenId.set("screen-2"), 50, TimeUnit.MILLISECONDS);

            var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
            var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                    "screenClass", "custom.WaitScreen",
                    "selector", Map.of("id", "wait-target"),
                    "condition", "screen_changed",
                    "timeoutMs", 500,
                    "pollIntervalMs", 10
            ));

            @SuppressWarnings("unchecked")
            var payload = (Map<String, Object>) result.value();
            assertEquals("wait-test", payload.get("driverId"));
            assertEquals(true, payload.get("matched"));
            assertEquals(false, payload.get("timedOut"));
            assertEquals(1, ((List<?>) payload.get("targets")).size());
            assertTrue(((Number) payload.get("elapsedMs")).longValue() >= 40);
        }
    }

    @Test
    void uiWaitPollsUntilFocusChangesWithinTimeout() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        var driver = new WaitTestUiDriver("custom.WaitScreen", true);
        mod.api().registerUiDriver(driver);
        mod.registerBuiltinProviders();

        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.schedule(() -> driver.focusedTargetId.set("wait-target"), 50, TimeUnit.MILLISECONDS);

            var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
            var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                    "screenClass", "custom.WaitScreen",
                    "selector", Map.of("id", "wait-target"),
                    "condition", "focus_changed",
                    "timeoutMs", 500,
                    "pollIntervalMs", 10
            ));

            @SuppressWarnings("unchecked")
            var payload = (Map<String, Object>) result.value();
            assertEquals("wait-test", payload.get("driverId"));
            assertEquals(true, payload.get("matched"));
            assertEquals(false, payload.get("timedOut"));
            assertEquals(1, ((List<?>) payload.get("targets")).size());
            assertTrue(((Number) payload.get("elapsedMs")).longValue() >= 40);
        }
    }

    @Test
    void uiWaitRequiresStableScreenForRequestedDuration() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiDriver(new WaitTestUiDriver("custom.WaitScreen", true));
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.WaitScreen",
                "selector", Map.of("id", "wait-target"),
                "condition", "screen_stable",
                "timeoutMs", 300,
                "pollIntervalMs", 10,
                "stableForMs", 60
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("wait-test", payload.get("driverId"));
        assertEquals(true, payload.get("matched"));
        assertEquals(false, payload.get("timedOut"));
        assertTrue(((Number) payload.get("elapsedMs")).longValue() >= 60);
    }

    @Test
    void uiWaitPollsUntilTargetTextChangesWithinTimeout() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        var driver = new WaitTestUiDriver("custom.WaitScreen", true);
        mod.api().registerUiDriver(driver);
        mod.registerBuiltinProviders();

        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.schedule(() -> driver.targetText.set("Updated Target"), 50, TimeUnit.MILLISECONDS);

            var tool = server.registry().findTool("moddev.ui_wait").orElseThrow();
            var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                    "screenClass", "custom.WaitScreen",
                    "selector", Map.of("id", "wait-target"),
                    "condition", "text_changed",
                    "timeoutMs", 500,
                    "pollIntervalMs", 10
            ));

            @SuppressWarnings("unchecked")
            var payload = (Map<String, Object>) result.value();
            assertEquals("wait-test", payload.get("driverId"));
            assertEquals(true, payload.get("matched"));
            assertEquals(false, payload.get("timedOut"));
            assertEquals(1, ((List<?>) payload.get("targets")).size());
            assertTrue(((Number) payload.get("elapsedMs")).longValue() >= 40);
        }
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
        assertEquals("minecraft", payload.get("modId"));
        assertEquals("slot", payload.get("role"));
        assertEquals("", payload.get("text"));
        assertEquals(Map.of("slotIndex", 0), payload.get("extensions"));
    }

    @Test
    void uiGetTooltipReturnsFailureWhenTargetIsMissing() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_get_tooltip").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "target", Map.of("id", "missing-target")
        ));

        assertFalse(result.success());
        assertEquals("target_not_found: selector did not match any target", result.error());
    }

    @Test
    void uiGetLiveScreenReturnsCurrentScreenMetrics() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new VanillaScreenUiDriver(
                registries.uiSessionStates(),
                registries.uiInteractionResolvers()
        ));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_get_live_screen").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of());

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(true, payload.get("active"));
        assertEquals("net.minecraft.client.gui.screens.TitleScreen", payload.get("screenClass"));
        assertEquals("vanilla-screen", payload.get("driverId"));
        assertEquals(320, payload.get("guiWidth"));
        assertEquals(480, payload.get("framebufferHeight"));
    }

    @Test
    void uiGetLiveScreenReturnsAllActiveDrivers() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new CompositeTestUiDriver(
                "base",
                100,
                "custom.CompositeScreen",
                List.of(buttonTarget("base", "play", "Play"))
        ));
        registries.uiDrivers().register(new CompositeTestUiDriver(
                "addon",
                300,
                "custom.CompositeScreen",
                List.of(buttonTarget("addon", "pin", "Pin"))
        ));
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics("custom.CompositeScreen", 320, 240, 854, 480)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_get_live_screen").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of());

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("addon", payload.get("driverId"));
        assertEquals(
                List.of("addon", "base"),
                driverIds((List<Map<String, Object>>) payload.get("drivers"))
        );
    }

    @Test
    void uiGetLiveScreenReturnsInactivePayloadWhenNoScreenIsOpen() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        new UiToolProvider(registries, new TestClientScreenProbe(
                new ClientScreenMetrics(null, 0, 0, 0, 0)
        )).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_get_live_screen").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of());

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(false, payload.get("active"));
        assertEquals("", payload.get("screenClass"));
        assertEquals("", payload.get("driverId"));
    }

    @Test
    void uiActionReturnsFailureWhenDriverRejectsAction() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new RejectingUiDriver("custom.RejectScreen"));
        new UiToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.RejectScreen",
                "action", "click",
                "target", Map.of("id", "reject-target")
        ));

        assertFalse(result.success());
        assertEquals("unsupported: action rejected by driver rejecting-test: Action disabled for test", result.error());
    }

    @Test
    void uiRunIntentReturnsUnsupportedIntentFromInputControllerWithoutDriverSpecialCase() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new FallbackRegionUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        registries.inputControllers().add(new RecordingInputController(OperationResult.rejected("unsupported_intent")));
        new UiToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_run_intent").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "intent", "inventory"
        ));

        assertFalse(result.success());
        assertEquals("unsupported_intent", result.error());
    }

    @Test
    void uiRunIntentDelegatesSupportedIntentToLaterInputControllerAndReturnsSnapshots() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.inputControllers().add(new RecordingInputController(OperationResult.rejected("unsupported_intent")));
        var controller = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(controller);
        var mod = new ModDevMCP(server, registries);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_run_intent").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "intent", "inventory"
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("run_intent", payload.get("action"));
        assertEquals("inventory", payload.get("intent"));
        assertEquals("ui_intent", controller.lastAction.get());
        assertFalse(payload.containsKey("controller"));
        assertTrue(payload.containsKey("preSnapshotRef"));
        assertTrue(payload.containsKey("postSnapshotRef"));
        assertTrue(payload.containsKey("postActionSnapshot"));
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
    void uiSwitchReturnsFailureWhenTargetSelectorMatchesNothing() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_switch").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "target", Map.of("id", "missing-target")
        ));

        assertFalse(result.success());
        assertEquals("target_not_found: selector did not match any target", result.error());
    }

    @Test
    void uiSwitchCanWaitForFocusChangeAfterAction() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var mod = new ModDevMCP(server, registries);
        mod.api().registerUiDriver(new WaitTestUiDriver("custom.WaitScreen", true));
        mod.registerBuiltinProviders();

        var tool = server.registry().findTool("moddev.ui_switch").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.WaitScreen",
                "target", Map.of("id", "wait-target"),
                "waitCondition", "focus_changed",
                "waitTimeoutMs", 500,
                "waitPollIntervalMs", 10
        ));

        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("wait-test", payload.get("driverId"));
        assertEquals("switch", payload.get("action"));
        assertEquals(true, ((Map<?, ?>) payload.get("wait")).get("matched"));
        assertEquals(false, ((Map<?, ?>) payload.get("wait")).get("timedOut"));
        assertEquals(1, ((List<?>) ((Map<?, ?>) payload.get("wait")).get("targets")).size());
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
    void uiRunIntentReturnsUnsupportedIntentForUnknownIntent() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new FallbackRegionUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        registries.inputControllers().add(new RecordingInputController(OperationResult.rejected("unsupported_intent")));
        new UiToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.ui_run_intent").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "intent", "totally_unknown_intent"
        ));

        assertFalse(result.success());
        assertEquals("unsupported_intent", result.error());
    }

    @Test
    void uiCloseClearsAndRunIntentLeavesSubsequentInteractionStateClosedWhenUnsupported() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        registries.uiDrivers().register(new FallbackRegionUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        registries.inputControllers().add(new RecordingInputController(OperationResult.rejected("unsupported_intent")));
        new UiToolProvider(registries).register(server.registry());

        var closeTool = server.registry().findTool("moddev.ui_close").orElseThrow();
        closeTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen"
        ));

        var stateTool = server.registry().findTool("moddev.ui_get_interaction_state").orElseThrow();
        var closedStateResult = stateTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen"
        ));

        @SuppressWarnings("unchecked")
        var closedPayload = (Map<String, Object>) closedStateResult.value();
        assertEquals(Map.of(), closedPayload.get("focusedTarget"));
        assertEquals("closed", closedPayload.get("selectionSource"));

        var runIntentTool = server.registry().findTool("moddev.ui_run_intent").orElseThrow();
        var runIntentResult = runIntentTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen",
                "intent", "inventory"
        ));
        assertFalse(runIntentResult.success());
        assertEquals("unsupported_intent", runIntentResult.error());

        var afterIntentStateResult = stateTool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", "custom.UnknownScreen"
        ));

        @SuppressWarnings("unchecked")
        var afterIntentPayload = (Map<String, Object>) afterIntentStateResult.value();
        assertEquals(Map.of(), afterIntentPayload.get("focusedTarget"));
        assertEquals("closed", afterIntentPayload.get("selectionSource"));
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

    private static final class RecordingPointerOffscreenCaptureProvider implements UiOffscreenCaptureProvider {

        private final String providerId;
        private final int priority;
        private final AtomicReference<Integer> lastMouseX = new AtomicReference<>(null);
        private final AtomicReference<Integer> lastMouseY = new AtomicReference<>(null);

        private RecordingPointerOffscreenCaptureProvider(String providerId, int priority) {
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
            lastMouseX.set(context.mouseX());
            lastMouseY.set(context.mouseY());
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

    private static final class FixedImageOffscreenCaptureProvider implements UiOffscreenCaptureProvider {

        private final String providerId;
        private final int priority;
        private final int width;
        private final int height;
        private final Color fill;

        private FixedImageOffscreenCaptureProvider(String providerId, int priority, int width, int height, Color fill) {
            this.providerId = providerId;
            this.priority = priority;
            this.width = width;
            this.height = height;
            this.fill = fill;
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
            var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            try {
                graphics.setColor(fill);
                graphics.fillRect(0, 0, width, height);
            } finally {
                graphics.dispose();
            }
            try {
                var output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                return new UiCaptureImage(providerId, "offscreen", output.toByteArray(), width, height, Map.of(
                        "guiWidth", width,
                        "guiHeight", height
                ));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    private static final class RecordingRefUiDriver implements UiDriver {

        private final String screenClass;
        private final String targetId;
        private final AtomicReference<String> lastAction = new AtomicReference<>(null);
        private final AtomicReference<String> lastActionTargetId = new AtomicReference<>(null);
        private final AtomicReference<String> lastContextScreenClass = new AtomicReference<>(null);
        private final AtomicReference<String> lastContextModId = new AtomicReference<>(null);
        private final java.util.concurrent.atomic.AtomicInteger actionCount = new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger snapshotCount = new java.util.concurrent.atomic.AtomicInteger();

        private RecordingRefUiDriver(String screenClass) {
            this(screenClass, "ref-target");
        }

        private RecordingRefUiDriver(String screenClass, String targetId) {
            this.screenClass = screenClass;
            this.targetId = targetId;
        }

        @Override
        public DriverDescriptor descriptor() {
            return new DriverDescriptor("recording-ref-driver-" + screenClass, "test", 10_000, Set.of("snapshot", "query", "action"));
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            snapshotCount.incrementAndGet();
            return new UiSnapshot(
                    "ref-screen-1",
                    context.screenClass(),
                    descriptor().id(),
                    query(context, dev.vfyjxf.mcp.api.ui.TargetSelector.builder().build()),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, dev.vfyjxf.mcp.api.ui.TargetSelector selector) {
            return List.of(new UiTarget(
                    targetId,
                    descriptor().id(),
                    context.screenClass(),
                    context.modId(),
                    "button",
                    "Ref Target",
                    new Bounds(10, 20, 40, 20),
                    UiTargetState.defaultState(),
                    List.of("click", "hover"),
                    Map.of()
            )).stream().filter(target ->
                    (selector.id() == null || selector.id().equals(target.targetId()))
                            && (selector.role() == null || selector.role().equals(target.role()))
            ).toList();
        }

        @Override
        public dev.vfyjxf.mcp.api.model.OperationResult<Map<String, Object>> action(UiContext context, dev.vfyjxf.mcp.api.ui.UiActionRequest request) {
            actionCount.incrementAndGet();
            lastAction.set(request.action());
            lastActionTargetId.set(request.target().id());
            lastContextScreenClass.set(context.screenClass());
            lastContextModId.set(context.modId());
            return dev.vfyjxf.mcp.api.model.OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true
            ));
        }

        @Override
        public dev.vfyjxf.mcp.api.model.OperationResult<Map<String, Object>> capture(UiContext context, CaptureRequest request) {
            return dev.vfyjxf.mcp.api.model.OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "performed", true
            ));
        }
    }

    private static final class CompositeTestUiDriver implements UiDriver {

        private final DriverDescriptor descriptor;
        private final String screenClass;
        private final List<UiTarget> targets;

        private CompositeTestUiDriver(String driverId, int priority, String screenClass, List<UiTarget> targets) {
            this.descriptor = new DriverDescriptor(driverId, "test", priority, Set.of("snapshot", "query", "action"));
            this.screenClass = screenClass;
            this.targets = List.copyOf(targets);
        }

        @Override
        public DriverDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    "screen",
                    context.screenClass(),
                    descriptor.id(),
                    targets,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, TargetSelector selector) {
            return targets.stream()
                    .filter(target -> selector.role() == null || selector.role().equals(target.role()))
                    .filter(target -> selector.id() == null || selector.id().equals(target.targetId()))
                    .toList();
        }

        @Override
        public OperationResult<Map<String, Object>> action(UiContext context, UiActionRequest request) {
            return OperationResult.success(Map.of(
                    "driverId", descriptor.id(),
                    "performed", true,
                    "action", request.action()
            ));
        }
    }

    private static final class MutableRefUiDriver implements UiDriver {

        private final String screenClass;
        private final AtomicReference<String> screenId;
        private final AtomicReference<String> targetId;
        private final java.util.concurrent.atomic.AtomicInteger actionCount = new java.util.concurrent.atomic.AtomicInteger();

        private MutableRefUiDriver(String screenClass, String initialScreenId, String initialTargetId) {
            this.screenClass = screenClass;
            this.screenId = new AtomicReference<>(initialScreenId);
            this.targetId = new AtomicReference<>(initialTargetId);
        }

        @Override
        public DriverDescriptor descriptor() {
            return new DriverDescriptor("mutable-ref-driver-" + screenClass, "test", 10_000, Set.of("snapshot", "query", "action"));
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    screenId.get(),
                    context.screenClass(),
                    descriptor().id(),
                    query(context, dev.vfyjxf.mcp.api.ui.TargetSelector.builder().build()),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, dev.vfyjxf.mcp.api.ui.TargetSelector selector) {
            return List.of(new UiTarget(
                    targetId.get(),
                    descriptor().id(),
                    context.screenClass(),
                    context.modId(),
                    "button",
                    "Mutable Ref Target",
                    new Bounds(10, 20, 40, 20),
                    UiTargetState.defaultState(),
                    List.of("click"),
                    Map.of()
            )).stream().filter(target ->
                    (selector.id() == null || selector.id().equals(target.targetId()))
                            && (selector.role() == null || selector.role().equals(target.role()))
            ).toList();
        }

        @Override
        public dev.vfyjxf.mcp.api.model.OperationResult<Map<String, Object>> action(UiContext context, dev.vfyjxf.mcp.api.ui.UiActionRequest request) {
            actionCount.incrementAndGet();
            return dev.vfyjxf.mcp.api.model.OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true
            ));
        }
    }

    private static final class RecordingInputController implements InputController {

        private final OperationResult<Void> result;
        private final AtomicReference<String> lastAction = new AtomicReference<>(null);

        private RecordingInputController(OperationResult<Void> result) {
            this.result = result;
        }

        @Override
        public OperationResult<Void> perform(String action, Map<String, Object> arguments) {
            lastAction.set(action);
            return result;
        }
    }

    private static final class InspectActionUiDriver implements UiDriver {

        private final String screenClass;
        private final AtomicReference<String> lastActionTargetId = new AtomicReference<>(null);

        private InspectActionUiDriver(String screenClass) {
            this.screenClass = screenClass;
        }

        @Override
        public DriverDescriptor descriptor() {
            return new DriverDescriptor("inspect-action-driver", "test", 10_000, Set.of("snapshot", "query", "action"));
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    "inspect-screen-1",
                    context.screenClass(),
                    descriptor().id(),
                    targets(context),
                    List.of(),
                    "launch-button",
                    null,
                    "disabled-button",
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, dev.vfyjxf.mcp.api.ui.TargetSelector selector) {
            return targets(context).stream()
                    .filter(target -> selector.id() == null || selector.id().equals(target.targetId()))
                    .filter(target -> selector.role() == null || selector.role().equals(target.role()))
                    .filter(target -> selector.text() == null || selector.text().equals(target.text()))
                    .toList();
        }

        @Override
        public OperationResult<Map<String, Object>> action(UiContext context, dev.vfyjxf.mcp.api.ui.UiActionRequest request) {
            lastActionTargetId.set(request.target().id());
            return OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true
            ));
        }

        @Override
        public UiInspectResult inspect(UiContext context, SnapshotOptions options) {
            return new UiInspectResult(
                    context.screenClass(),
                    "inspect-screen-1",
                    descriptor().id(),
                    Map.of(
                            "targetCount", 2,
                            "actionableCount", 1
                    ),
                    targets(context),
                    Map.of(
                            "focusedTargetId", "launch-button",
                            "hoveredTargetId", "disabled-button"
                    ),
                    null
            );
        }

        private List<UiTarget> targets(UiContext context) {
            return List.of(
                    new UiTarget(
                            "launch-button",
                            descriptor().id(),
                            context.screenClass(),
                            context.modId(),
                            "button",
                            "Launch",
                            new Bounds(10, 20, 40, 20),
                            UiTargetState.defaultState(),
                            List.of("click"),
                            Map.of()
                    ),
                    new UiTarget(
                            "disabled-button",
                            descriptor().id(),
                            context.screenClass(),
                            context.modId(),
                            "button",
                            "Disabled",
                            new Bounds(60, 20, 40, 20),
                            new UiTargetState(true, false, false, false, false, false),
                            List.of("click"),
                            Map.of()
                    )
            );
        }
    }

    private static final class WaitTestUiDriver implements UiDriver {

        private static final DriverDescriptor DESCRIPTOR = new DriverDescriptor(
                "wait-test",
                "test",
                10_000,
                Set.of("snapshot", "query")
        );

        private final String screenClass;
        private final AtomicBoolean visible;
        private final AtomicReference<String> screenId = new AtomicReference<>("screen-1");
        private final AtomicReference<String> focusedTargetId = new AtomicReference<>(null);
        private final AtomicReference<String> targetText = new AtomicReference<>("Wait Target");

        private WaitTestUiDriver(String screenClass, boolean visible) {
            this.screenClass = screenClass;
            this.visible = new AtomicBoolean(visible);
        }

        @Override
        public DriverDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            var targets = query(context, dev.vfyjxf.mcp.api.ui.TargetSelector.builder().build());
            return new UiSnapshot(
                    screenId.get(),
                    context.screenClass(),
                    DESCRIPTOR.id(),
                    targets,
                    List.of(),
                    focusedTargetId.get(),
                    null,
                    null,
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, dev.vfyjxf.mcp.api.ui.TargetSelector selector) {
            if (!visible.get()) {
                return List.of();
            }
            return List.of(new UiTarget(
                    "wait-target",
                    DESCRIPTOR.id(),
                    context.screenClass(),
                    context.modId(),
                    "region",
                    targetText.get(),
                    new Bounds(0, 0, 10, 10),
                    UiTargetState.defaultState(),
                    List.of("focus"),
                    Map.of()
            )).stream().filter(target ->
                    (selector.id() == null || selector.id().equals(target.targetId()))
                            && (selector.role() == null || selector.role().equals(target.role()))
            ).toList();
        }

        @Override
        public dev.vfyjxf.mcp.api.model.OperationResult<Map<String, Object>> action(UiContext context, dev.vfyjxf.mcp.api.ui.UiActionRequest request) {
            var targetId = request.target().id() == null ? "wait-target" : request.target().id();
            if ("switch".equals(request.action())) {
                Thread.ofVirtual().start(() -> {
                    sleepQuietly(50);
                    focusedTargetId.set(targetId);
                });
            }
            if ("type".equals(request.action()) && request.arguments().get("text") instanceof String text) {
                Thread.ofVirtual().start(() -> {
                    sleepQuietly(50);
                    targetText.set(text);
                });
            }
            return dev.vfyjxf.mcp.api.model.OperationResult.success(Map.of(
                    "driverId", DESCRIPTOR.id(),
                    "action", request.action(),
                    "performed", true
            ));
        }

        private void sleepQuietly(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }
    }

    private static final class DriverWaitUiDriver implements UiDriver {

        private final String screenClass;

        private DriverWaitUiDriver(String screenClass) {
            this.screenClass = screenClass;
        }

        @Override
        public DriverDescriptor descriptor() {
            return new DriverDescriptor("driver-wait", "test", 10_000, Set.of("snapshot", "query"));
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    "wait-api-screen",
                    context.screenClass(),
                    descriptor().id(),
                    query(context, dev.vfyjxf.mcp.api.ui.TargetSelector.builder().build()),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, dev.vfyjxf.mcp.api.ui.TargetSelector selector) {
            return List.of(new UiTarget(
                    "wait-target",
                    descriptor().id(),
                    context.screenClass(),
                    context.modId(),
                    "button",
                    "Wait Target",
                    new Bounds(10, 20, 40, 20),
                    UiTargetState.defaultState(),
                    List.of("click"),
                    Map.of()
            ));
        }

        @Override
        public UiWaitResult waitFor(UiContext context, UiWaitRequest request) {
            return new UiWaitResult(
                    false,
                    75L,
                    query(context, dev.vfyjxf.mcp.api.ui.TargetSelector.builder().build()).getFirst(),
                    "timeout",
                    Map.of("condition", request.condition())
            );
        }
    }

    private static final class RejectingUiDriver implements UiDriver {

        private static final DriverDescriptor DESCRIPTOR = new DriverDescriptor(
                "rejecting-test",
                "test",
                10_000,
                Set.of("snapshot", "query")
        );

        private final String screenClass;

        private RejectingUiDriver(String screenClass) {
            this.screenClass = screenClass;
        }

        @Override
        public DriverDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    "reject-screen",
                    context.screenClass(),
                    DESCRIPTOR.id(),
                    query(context, dev.vfyjxf.mcp.api.ui.TargetSelector.builder().build()),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, dev.vfyjxf.mcp.api.ui.TargetSelector selector) {
            return List.of(new UiTarget(
                    "reject-target",
                    DESCRIPTOR.id(),
                    context.screenClass(),
                    context.modId(),
                    "button",
                    "Reject",
                    new Bounds(0, 0, 20, 20),
                    UiTargetState.defaultState(),
                    List.of("click"),
                    Map.of()
            )).stream().filter(target ->
                    (selector.id() == null || selector.id().equals(target.targetId()))
                            && (selector.role() == null || selector.role().equals(target.role()))
            ).toList();
        }

        @Override
        public dev.vfyjxf.mcp.api.model.OperationResult<Map<String, Object>> action(UiContext context, dev.vfyjxf.mcp.api.ui.UiActionRequest request) {
            return dev.vfyjxf.mcp.api.model.OperationResult.rejected("Action disabled for test");
        }

        @Override
        public dev.vfyjxf.mcp.api.model.OperationResult<List<UiTarget>> inspectAt(UiContext context, int x, int y) {
            return dev.vfyjxf.mcp.api.model.OperationResult.rejected("Inspect disabled for test");
        }
    }

    private record TestClientScreenProbe(ClientScreenMetrics metrics) implements ClientScreenProbe {
    }

    private static final class MutableClientScreenProbe implements ClientScreenProbe {

        private final AtomicReference<ClientScreenMetrics> metrics;

        private MutableClientScreenProbe(ClientScreenMetrics initialMetrics) {
            this.metrics = new AtomicReference<>(initialMetrics);
        }

        @Override
        public ClientScreenMetrics metrics() {
            return metrics.get();
        }
    }

    private static UiTarget buttonTarget(String driverId, String targetId, String text) {
        return new UiTarget(
                targetId,
                driverId,
                "custom.CompositeScreen",
                "minecraft",
                "button",
                text,
                new Bounds(10, 10, 40, 20),
                UiTargetState.defaultState(),
                List.of("click"),
                Map.of()
        );
    }

    private static Set<String> targetKeys(List<Map<String, Object>> targets) {
        return targets.stream()
                .map(target -> target.get("driverId") + ":" + target.get("targetId"))
                .collect(java.util.stream.Collectors.toSet());
    }

    private static List<String> driverIds(List<Map<String, Object>> drivers) {
        return drivers.stream()
                .map(driver -> String.valueOf(driver.get("driverId")))
                .toList();
    }
}
