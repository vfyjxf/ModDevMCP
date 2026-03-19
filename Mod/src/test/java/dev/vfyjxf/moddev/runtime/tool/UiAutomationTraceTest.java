package dev.vfyjxf.moddev.runtime.tool;

import dev.vfyjxf.moddev.ModDevMCP;
import dev.vfyjxf.moddev.api.runtime.*;
import dev.vfyjxf.moddev.api.ui.*;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.server.ModDevMcpServer;
import dev.vfyjxf.moddev.server.api.ToolCallContext;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UiAutomationTraceTest {

    @Test
    void uiTraceGetReturnsRecordedBatchSteps() {
        var runtime = new RuntimeHarness();
        var session = runtime.openSession();
        runtime.server.registry().findTool("moddev.ui_batch").orElseThrow().handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "steps", List.of(
                        Map.of("type", "clickRef", "refId", session.refId()),
                        Map.of("type", "waitFor", "refId", session.refId(), "condition", "appeared", "timeoutMs", 50)
                )
        ));

        var tool = runtime.server.registry().findTool("moddev.ui_trace_get").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of("sessionId", session.sessionId()));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        @SuppressWarnings("unchecked")
        var traces = (List<Map<String, Object>>) payload.get("traces");
        assertFalse(traces.isEmpty());
        assertEquals("clickRef", traces.getFirst().get("type"));
        assertEquals(true, traces.getFirst().get("success"));
        assertTrue(((Number) traces.getFirst().get("elapsedMs")).longValue() >= 0L);
    }

    @Test
    void uiTraceGetIncludesErrorCodeForFailedBatchStep() {
        var runtime = new RuntimeHarness();
        var session = runtime.openSession();
        runtime.server.registry().findTool("moddev.ui_batch").orElseThrow().handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "stopOnError", true,
                "steps", List.of(
                        Map.of("type", "clickRef", "refId", "missing-ref")
                )
        ));

        var tool = runtime.server.registry().findTool("moddev.ui_trace_get").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of("sessionId", session.sessionId()));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        @SuppressWarnings("unchecked")
        var traces = (List<Map<String, Object>>) payload.get("traces");
        assertEquals("target_not_found", traces.getFirst().get("errorCode"));
        assertEquals(false, traces.getFirst().get("success"));
        assertTrue(((Number) traces.getFirst().get("elapsedMs")).longValue() >= 0L);
    }

    @Test
    void uiTraceGetNormalizesErrorCodeAndSeparatesMessage() {
        var runtime = new RuntimeHarness();
        var session = runtime.openSession();
        runtime.server.registry().findTool("moddev.ui_batch").orElseThrow().handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "stopOnError", true,
                "steps", List.of(Map.of())
        ));

        var tool = runtime.server.registry().findTool("moddev.ui_trace_get").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of("sessionId", session.sessionId()));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        @SuppressWarnings("unchecked")
        var traces = (List<Map<String, Object>>) payload.get("traces");
        assertEquals("invalid_input", traces.getFirst().get("errorCode"));
        assertEquals("batch step missing type", traces.getFirst().get("errorMessage"));
    }

    @Test
    void uiTraceGetIncludesStandaloneSessionToolSteps() {
        var runtime = new RuntimeHarness();
        var session = runtime.openSession();

        var clickTool = runtime.server.registry().findTool("moddev.ui_click_ref").orElseThrow();
        var clickResult = clickTool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "refId", session.refId()
        ));

        assertTrue(clickResult.success());

        var traceTool = runtime.server.registry().findTool("moddev.ui_trace_get").orElseThrow();
        var traceResult = traceTool.handler().handle(ToolCallContext.empty(), Map.of("sessionId", session.sessionId()));

        assertTrue(traceResult.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) traceResult.value();
        @SuppressWarnings("unchecked")
        var traces = (List<Map<String, Object>>) payload.get("traces");
        assertEquals(1, traces.size());
        assertEquals("clickRef", traces.getFirst().get("type"));
        assertEquals(true, traces.getFirst().get("success"));
    }

    @Test
    void uiTraceRecentReturnsOnlyLatestEntries() {
        var runtime = new RuntimeHarness();
        var session = runtime.openSession();
        runtime.server.registry().findTool("moddev.ui_batch").orElseThrow().handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "steps", List.of(
                        Map.of("type", "clickRef", "refId", session.refId()),
                        Map.of("type", "waitFor", "refId", session.refId(), "condition", "appeared", "timeoutMs", 50)
                )
        ));

        var tool = runtime.server.registry().findTool("moddev.ui_trace_recent").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "limit", 1
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        @SuppressWarnings("unchecked")
        var traces = (List<Map<String, Object>>) payload.get("traces");
        assertEquals(1, traces.size());
        assertEquals("waitFor", traces.getFirst().get("type"));
    }

    private record SessionHandle(String sessionId, String refId) {
    }

    private static final class RuntimeHarness {

        private final ModDevMcpServer server = new ModDevMcpServer(new McpToolRegistry());
        private final RuntimeRegistries registries = new RuntimeRegistries();

        private RuntimeHarness() {
            registries.uiDrivers().register(new TraceUiDriver("custom.TraceScreen"));
            new UiToolProvider(registries, new TestClientScreenProbe(
                    new ClientScreenMetrics("custom.TraceScreen", 320, 240, 854, 480)
            )).register(server.registry());
            new ModDevMCP(server, registries);
        }

        private SessionHandle openSession() {
            var tool = server.registry().findTool("moddev.ui_session_open").orElseThrow();
            var result = tool.handler().handle(ToolCallContext.empty(), Map.of());
            @SuppressWarnings("unchecked")
            var payload = (Map<String, Object>) result.value();
            @SuppressWarnings("unchecked")
            var ref = (Map<String, Object>) ((List<?>) payload.get("refs")).getFirst();
            return new SessionHandle((String) payload.get("sessionId"), (String) ref.get("refId"));
        }
    }

    private static final class TraceUiDriver implements UiDriver {

        private final String screenClass;

        private TraceUiDriver(String screenClass) {
            this.screenClass = screenClass;
        }

        @Override
        public DriverDescriptor descriptor() {
            return new DriverDescriptor("trace-driver", "test", 10_000, Set.of("snapshot", "query", "action"));
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    "trace-screen-1",
                    context.screenClass(),
                    descriptor().id(),
                    query(context, dev.vfyjxf.moddev.api.ui.TargetSelector.builder().build()),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, dev.vfyjxf.moddev.api.ui.TargetSelector selector) {
            return List.of(new UiTarget(
                    "trace-target",
                    descriptor().id(),
                    context.screenClass(),
                    context.modId(),
                    "button",
                    "Trace Target",
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
        public dev.vfyjxf.moddev.api.model.OperationResult<Map<String, Object>> action(UiContext context, dev.vfyjxf.moddev.api.ui.UiActionRequest request) {
            return dev.vfyjxf.moddev.api.model.OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true
            ));
        }
    }

    private record TestClientScreenProbe(ClientScreenMetrics metrics) implements ClientScreenProbe {
    }
}

