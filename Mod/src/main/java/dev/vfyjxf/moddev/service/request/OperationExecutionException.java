package dev.vfyjxf.moddev.service.request;

import java.util.Objects;

public final class OperationExecutionException extends RuntimeException {

    private final OperationError error;

    public OperationExecutionException(OperationError error) {
        super(Objects.requireNonNull(error, "error").errorMessage());
        this.error = error;
    }

    public OperationError error() {
        return error;
    }
}

