package dev.vfyjxf.mcp.service.runtime;

import dev.vfyjxf.mcp.service.operation.OperationDefinition;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;
import dev.vfyjxf.mcp.service.request.OperationExecutionException;
import dev.vfyjxf.mcp.service.request.OperationRequest;
import dev.vfyjxf.mcp.server.api.ToolResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RuntimeOperationBindings {

    @FunctionalInterface
    public interface OperationHandler {
        Map<String, Object> execute(Map<String, Object> input, String resolvedTargetSide) throws Exception;
    }

    @FunctionalInterface
    public interface ToolOperationInvoker {
        ToolResult invoke(String toolName, String targetSide, Map<String, Object> input) throws Exception;
    }

    @FunctionalInterface
    public interface StatusSnapshotProvider {
        StatusSnapshot snapshot();
    }

    public record StatusSnapshot(
            boolean serviceReady,
            boolean gameReady,
            List<String> connectedSides,
            String usageSkillId,
            Path exportRoot,
            String lastError
    ) {
    }

    record OperationBinding(
            OperationDefinition definition,
            OperationHandler handler
    ) {
    }

    private final OperationRegistry operationRegistry;
    private final OperationExecutorRegistry executorRegistry;

    public RuntimeOperationBindings(ToolOperationInvoker toolInvoker, StatusSnapshotProvider statusSnapshotProvider) {
        Objects.requireNonNull(toolInvoker, "toolInvoker");
        Objects.requireNonNull(statusSnapshotProvider, "statusSnapshotProvider");

        var bindings = new ArrayList<OperationBinding>();
        bindings.addAll(StatusOperationHandlers.operations(toolInvoker, statusSnapshotProvider));
        bindings.addAll(UiOperationHandlers.operations(toolInvoker));
        bindings.addAll(CommandOperationHandlers.operations(toolInvoker));
        bindings.addAll(InputOperationHandlers.operations(toolInvoker));
        bindings.addAll(WorldOperationHandlers.operations(toolInvoker));
        bindings.addAll(HotswapOperationHandlers.operations(toolInvoker));

        var definitions = new ArrayList<OperationDefinition>(bindings.size());
        var byId = new LinkedHashMap<String, OperationExecutorRegistry.OperationExecutor>(bindings.size());
        for (var binding : bindings) {
            definitions.add(binding.definition());
            byId.put(binding.definition().operationId(), binding.handler()::execute);
        }
        this.operationRegistry = new OperationRegistry(definitions);
        this.executorRegistry = new OperationExecutorRegistry(byId);
    }

    public OperationRegistry operationRegistry() {
        return operationRegistry;
    }

    public Map<String, Object> execute(OperationRequest request, String resolvedTargetSide) throws Exception {
        return executorRegistry.execute(request.operationId(), request.input(), resolvedTargetSide);
    }

    static OperationBinding binding(OperationDefinition definition, OperationHandler handler) {
        return new OperationBinding(definition, handler);
    }

    static OperationHandler toolHandler(ToolOperationInvoker invoker, String toolName) {
        return (input, resolvedTargetSide) -> invokeTool(invoker, toolName, resolvedTargetSide, input);
    }

    static Map<String, Object> invokeTool(
            ToolOperationInvoker invoker,
            String toolName,
            String resolvedTargetSide,
            Map<String, Object> input
    ) throws Exception {
        var result = invoker.invoke(toolName, resolvedTargetSide, input);
        if (!result.success()) {
            throw executionFailure(normalizeMessage(result.error(), toolName + " failed"));
        }
        if (result.value() instanceof Map<?, ?> mapValue) {
            @SuppressWarnings("unchecked")
            var payload = (Map<String, Object>) mapValue;
            return payload;
        }
        return Map.of("value", result.value());
    }

    static OperationExecutionException executionFailure(String message) {
        return new OperationExecutionException(new dev.vfyjxf.mcp.service.request.OperationError(
                "operation_execution_failed",
                normalizeMessage(message, "operation execution failed")
        ));
    }

    private static String normalizeMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }

    static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return Map.copyOf(schema);
    }
}
