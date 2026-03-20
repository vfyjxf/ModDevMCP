package dev.vfyjxf.moddev.api.operation;

import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.Objects;

public record OperationRegistration(
        OperationDefinition definition,
        OperationExecutor executor
) {
    public OperationRegistration {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(executor, "executor");
    }
}

