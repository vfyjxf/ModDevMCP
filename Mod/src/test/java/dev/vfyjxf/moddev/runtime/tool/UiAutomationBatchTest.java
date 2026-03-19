package dev.vfyjxf.moddev.runtime.tool;

import dev.vfyjxf.moddev.ModDevMCP;
import dev.vfyjxf.moddev.api.runtime.*;
import dev.vfyjxf.moddev.api.ui.*;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.runtime.ui.UiCaptureRenderer;
import dev.vfyjxf.moddev.server.ModDevMcpServer;
import dev.vfyjxf.moddev.server.api.ToolCallContext;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiAutomationBatchTest {

    @Test
    void uiBatchExecutesClickWaitAndScreenshotSteps() {
        var runtime = new RuntimeHarness();
        var session = runtime.openSession();

        var tool = runtime.server.registry().findTool("moddev.ui_batch").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "steps", List.of(
                        Map.of("type", "clickRef", "refId", session.refId()),
                        Map.of("type", "waitFor", "refId", session.refId(), "condition", "appeared", "timeoutMs", 50),
                        Map.of("type", "screenshot", "refId", session.refId())
                )
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(true, payload.get("success"));
        assertEquals(3, ((List<?>) payload.get("steps")).size());
    }

    @Test
    void uiBatchStopsOnFirstFailureWhenConfigured() {
        var runtime = new RuntimeHarness();
        var session = runtime.openSession();

        var tool = runtime.server.registry().findTool("moddev.ui_batch").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "stopOnError", true,
                "steps", List.of(
                        Map.of("type", "clickRef", "refId", session.refId()),
                        Map.of("type", "clickRef", "refId", "missing-ref"),
                        Map.of("type", "screenshot", "refId", session.refId())
                )
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(false, payload.get("success"));
        assertEquals("batch_step_failed", payload.get("errorCode"));
        assertEquals(1, payload.get("failureStepIndex"));
        @SuppressWarnings("unchecked")
        var steps = (List<Map<String, Object>>) payload.get("steps");
        assertEquals(2, steps.size());
        assertEquals("target_not_found", steps.get(1).get("errorCode"));
        assertEquals(0, runtime.driver.captureCount.get());
    }

    @Test
    void uiBatchMarksOverallResultFailedWhenContinuingAfterError() {
        var runtime = new RuntimeHarness();
        var session = runtime.openSession();

        var tool = runtime.server.registry().findTool("moddev.ui_batch").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "stopOnError", false,
                "steps", List.of(
                        Map.of("type", "clickRef", "refId", session.refId()),
                        Map.of("type", "clickRef", "refId", "missing-ref"),
                        Map.of("type", "screenshot", "refId", session.refId())
                )
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(false, payload.get("success"));
        assertEquals("batch_step_failed", payload.get("errorCode"));
        assertEquals(1, payload.get("failureStepIndex"));
        @SuppressWarnings("unchecked")
        var steps = (List<Map<String, Object>>) payload.get("steps");
        assertEquals(3, steps.size());
        assertEquals(false, steps.get(1).get("success"));
        assertEquals(1, runtime.driver.captureCount.get());
    }

    @Test
    void uiBatchNormalizesStepErrorCodeAndSeparatesMessage() {
        var runtime = new RuntimeHarness();
        var session = runtime.openSession();

        var tool = runtime.server.registry().findTool("moddev.ui_batch").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "sessionId", session.sessionId(),
                "stopOnError", true,
                "steps", List.of(Map.of())
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(false, payload.get("success"));
        @SuppressWarnings("unchecked")
        var steps = (List<Map<String, Object>>) payload.get("steps");
        assertEquals("invalid_input", steps.getFirst().get("errorCode"));
        assertEquals("batch step missing type", steps.getFirst().get("errorMessage"));
    }

    private record SessionHandle(String sessionId, String refId) {
    }

    private static final class RuntimeHarness {

        private final ModDevMcpServer server = new ModDevMcpServer(new McpToolRegistry());
        private final RuntimeRegistries registries = new RuntimeRegistries();
        private final ModDevMCP mod = new ModDevMCP(server, registries);
        private final RecordingBatchUiDriver driver = new RecordingBatchUiDriver("custom.BatchScreen");

        private RuntimeHarness() {
            registries.uiDrivers().register(driver);
            mod.api().registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("offscreen-test", 500));
            new UiToolProvider(registries, new TestClientScreenProbe(
                    new ClientScreenMetrics("custom.BatchScreen", 320, 240, 854, 480)
            )).register(server.registry());
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

    private static final class RecordingBatchUiDriver implements UiDriver {

        private final String screenClass;
        private final AtomicReference<String> lastAction = new AtomicReference<>(null);
        private final java.util.concurrent.atomic.AtomicInteger captureCount = new java.util.concurrent.atomic.AtomicInteger();

        private RecordingBatchUiDriver(String screenClass) {
            this.screenClass = screenClass;
        }

        @Override
        public DriverDescriptor descriptor() {
            return new DriverDescriptor("batch-driver", "test", 10_000, Set.of("snapshot", "query", "action", "capture"));
        }

        @Override
        public boolean matches(UiContext context) {
            return screenClass.equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    "batch-screen-1",
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
                    "batch-target",
                    descriptor().id(),
                    context.screenClass(),
                    context.modId(),
                    "button",
                    "Batch Target",
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
        public dev.vfyjxf.moddev.api.model.OperationResult<Map<String, Object>> action(UiContext context, dev.vfyjxf.moddev.api.ui.UiActionRequest request) {
            lastAction.set(request.action());
            return dev.vfyjxf.moddev.api.model.OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true
            ));
        }

        @Override
        public dev.vfyjxf.moddev.api.model.OperationResult<Map<String, Object>> capture(UiContext context, CaptureRequest request) {
            captureCount.incrementAndGet();
            return dev.vfyjxf.moddev.api.model.OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "performed", true
            ));
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

    private record TestClientScreenProbe(ClientScreenMetrics metrics) implements ClientScreenProbe {
    }
}

