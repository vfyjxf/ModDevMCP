package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.api.operation.OperationRegistration;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;
import dev.vfyjxf.moddev.service.operation.OperationRegistry;
import dev.vfyjxf.moddev.service.request.OperationExecutionException;
import dev.vfyjxf.moddev.service.request.OperationRequest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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

    public RuntimeOperationBindings(RuntimeRegistries registries, StatusSnapshotProvider statusSnapshotProvider) {
        this(registries, statusSnapshotProvider, registries.operationRegistrations());
    }

    public RuntimeOperationBindings(
            RuntimeRegistries registries,
            StatusSnapshotProvider statusSnapshotProvider,
            Collection<OperationRegistration> operationRegistrations
    ) {
        Objects.requireNonNull(registries, "registries");
        Objects.requireNonNull(statusSnapshotProvider, "statusSnapshotProvider");
        Objects.requireNonNull(operationRegistrations, "operationRegistrations");

        var bindings = new ArrayList<OperationBinding>();
        bindings.addAll(StatusOperationHandlers.operations(registries, statusSnapshotProvider));
        bindings.addAll(UiOperationHandlers.operations(registries));
        bindings.addAll(CommandOperationHandlers.operations(registries));
        bindings.addAll(InputOperationHandlers.operations(registries));
        bindings.addAll(WorldOperationHandlers.operations(registries));
        bindings.addAll(HotswapOperationHandlers.operations(registries));

        var definitions = new ArrayList<OperationDefinition>(bindings.size() + operationRegistrations.size());
        var byId = new LinkedHashMap<String, OperationExecutorRegistry.OperationExecutor>(bindings.size() + operationRegistrations.size());
        for (var binding : bindings) {
            definitions.add(binding.definition());
            byId.put(binding.definition().operationId(), binding.handler()::execute);
        }
        for (var registration : operationRegistrations) {
            definitions.add(registration.definition());
            byId.put(registration.definition().operationId(), registration.executor()::execute);
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

    static OperationExecutionException executionFailure(String message) {
        return new OperationExecutionException(new dev.vfyjxf.moddev.service.request.OperationError(
                "operation_execution_failed",
                normalizeMessage(message, "operation execution failed")
        ));
    }

    static String normalizeMessage(String message, String fallback) {
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
