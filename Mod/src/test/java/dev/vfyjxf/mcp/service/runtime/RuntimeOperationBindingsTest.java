package dev.vfyjxf.mcp.service.runtime;

import dev.vfyjxf.mcp.service.request.OperationError;
import dev.vfyjxf.mcp.service.request.OperationExecutionException;
import dev.vfyjxf.mcp.service.request.OperationRequest;
import dev.vfyjxf.mcp.server.api.ToolResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeOperationBindingsTest {

    @Test
    void operationRegistryCoversExistingCapabilityAreas() {
        var bindings = new RuntimeOperationBindings(new RecordingToolInvoker(), statusProvider());

        var operationIds = bindings.operationRegistry().all().stream()
                .map(definition -> definition.operationId())
                .toList();

        assertTrue(operationIds.contains("status.get"));
        assertTrue(operationIds.contains("ui.inspect"));
        assertTrue(operationIds.contains("command.execute"));
        assertTrue(operationIds.contains("world.list"));
        assertTrue(operationIds.contains("hotswap.reload"));
    }

    @Test
    void statusOperationAllowsNullLastError() throws Exception {
        var bindings = new RuntimeOperationBindings(new RecordingToolInvoker(), statusProvider());

        var output = bindings.execute(
                new OperationRequest("req-status", "status.get", null, Map.of()),
                null
        );

        assertEquals(true, output.get("serviceReady"));
        assertEquals(null, output.get("lastError"));
    }

    @Test
    void sideAwareOperationsInvokeMatchingRuntimeTools() throws Exception {
        var invoker = new RecordingToolInvoker();
        invoker.result = ToolResult.success(Map.of("executed", true, "messages", List.of("ok")));
        var bindings = new RuntimeOperationBindings(invoker, statusProvider());

        var output = bindings.execute(
                new OperationRequest("req-1", "command.execute", "server", Map.of("command", "/say hi")),
                "server"
        );

        assertEquals("moddev.command_execute", invoker.calls.getFirst().toolName());
        assertEquals("server", invoker.calls.getFirst().targetSide());
        assertEquals(Map.of("command", "/say hi"), invoker.calls.getFirst().input());
        assertEquals(true, output.get("executed"));
    }

    @Test
    void hotswapFailuresReturnStructuredExecutionErrors() throws Exception {
        var invoker = new RecordingToolInvoker();
        invoker.result = ToolResult.success(Map.of(
                "success", false,
                "error", "Compilation failed with exit code 1"
        ));
        var bindings = new RuntimeOperationBindings(invoker, statusProvider());

        try {
            bindings.execute(
                    new OperationRequest("req-2", "hotswap.reload", "client", Map.of("compile", true)),
                    "client"
            );
        } catch (OperationExecutionException exception) {
            OperationError error = exception.error();
            assertEquals("operation_execution_failed", error.errorCode());
            assertEquals("Compilation failed with exit code 1", error.errorMessage());
            return;
        }
        throw new AssertionError("Expected structured execution error");
    }

    private static RuntimeOperationBindings.StatusSnapshotProvider statusProvider() {
        return () -> new RuntimeOperationBindings.StatusSnapshot(
                true,
                true,
                List.of("client", "server"),
                "moddev-entry",
                Path.of("build/export"),
                null
        );
    }

    private static final class RecordingToolInvoker implements RuntimeOperationBindings.ToolOperationInvoker {
        private final List<Invocation> calls = new ArrayList<>();
        private ToolResult result = ToolResult.success(Map.of());

        @Override
        public ToolResult invoke(String toolName, String targetSide, Map<String, Object> input) {
            calls.add(new Invocation(toolName, targetSide, input));
            return result;
        }
    }

    private record Invocation(
            String toolName,
            String targetSide,
            Map<String, Object> input
    ) {
    }
}
