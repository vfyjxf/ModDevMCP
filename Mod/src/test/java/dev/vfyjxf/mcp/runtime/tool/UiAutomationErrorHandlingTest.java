package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.api.ui.*;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UiAutomationErrorHandlingTest {

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
    void uiClickRefReturnsTargetStaleAfterLiveScreenChanges() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var registries = new RuntimeRegistries();
        var probe = new MutableClientScreenProbe(new ClientScreenMetrics(
                "custom.RefScreen",
                320,
                240,
                854,
                480
        ));
        registries.uiDrivers().register(new RefUiDriver("custom.RefScreen", "ref-target"));
        registries.uiDrivers().register(new RefUiDriver("custom.NextScreen", "next-target"));
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
    }

    private static final class RefUiDriver implements UiDriver {

        private final String screenClass;
        private final String targetId;

        private RefUiDriver(String screenClass, String targetId) {
            this.screenClass = screenClass;
            this.targetId = targetId;
        }

        @Override
        public DriverDescriptor descriptor() {
            return new DriverDescriptor("error-driver-" + screenClass, "test", 10_000, Set.of("snapshot", "query", "action"));
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    "screen-" + screenClass,
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
                    "Target",
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
            return dev.vfyjxf.mcp.api.model.OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true
            ));
        }
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
}
