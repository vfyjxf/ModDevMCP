package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.service.request.OperationError;
import dev.vfyjxf.moddev.service.request.OperationExecutionException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class OperationExecutorRegistry {

    @FunctionalInterface
    public interface OperationExecutor {
        Map<String, Object> execute(Map<String, Object> input, String resolvedTargetSide) throws Exception;
    }

    private final Map<String, OperationExecutor> executors;

    public OperationExecutorRegistry(Map<String, OperationExecutor> executors) {
        Objects.requireNonNull(executors, "executors");
        var byId = new LinkedHashMap<String, OperationExecutor>(executors.size());
        for (var entry : executors.entrySet()) {
            var operationId = Objects.requireNonNull(entry.getKey(), "operationId");
            if (operationId.isBlank()) {
                throw new IllegalArgumentException("operationId must not be blank");
            }
            byId.put(operationId, Objects.requireNonNull(entry.getValue(), "executor"));
        }
        this.executors = Map.copyOf(byId);
    }

    public Map<String, Object> execute(
            String operationId,
            Map<String, Object> input,
            String resolvedTargetSide
    ) throws Exception {
        var executor = executors.get(operationId);
        if (executor == null) {
            throw new OperationExecutionException(new OperationError("operation_not_found", "operation was not found"));
        }
        return executor.execute(input, resolvedTargetSide);
    }
}

