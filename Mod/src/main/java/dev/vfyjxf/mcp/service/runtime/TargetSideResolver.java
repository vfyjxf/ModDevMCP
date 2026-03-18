package dev.vfyjxf.mcp.service.runtime;

import dev.vfyjxf.mcp.service.operation.OperationDefinition;
import dev.vfyjxf.mcp.service.request.OperationError;

import java.util.Collection;
import java.util.Objects;

public final class TargetSideResolver {

    public String resolve(OperationDefinition operation, String requestedTargetSide, Collection<String> connectedSides) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(connectedSides, "connectedSides");

        if (!operation.supportsTargetSide()) {
            if (requestedTargetSide != null) {
                throw fail("target_side_not_supported", "operation does not support target side selection");
            }
            return null;
        }

        if (requestedTargetSide != null) {
            if (!operation.availableTargetSides().contains(requestedTargetSide)) {
                throw fail("target_side_unsupported", "target side is not supported by operation");
            }
            if (!connectedSides.contains(requestedTargetSide)) {
                throw fail("target_side_disconnected", "target side is not currently connected");
            }
            return requestedTargetSide;
        }

        String resolved = null;
        int eligibleCount = 0;
        for (var side : operation.availableTargetSides()) {
            if (connectedSides.contains(side)) {
                resolved = side;
                eligibleCount++;
            }
        }
        if (eligibleCount == 1) {
            return resolved;
        }
        if (eligibleCount > 1) {
            throw fail("target_side_required", "targetSide is required when multiple sides are eligible");
        }
        throw fail("target_side_disconnected", "no eligible target side is currently connected");
    }

    private static TargetSideResolutionException fail(String errorCode, String errorMessage) {
        return new TargetSideResolutionException(new OperationError(errorCode, errorMessage));
    }

    public static final class TargetSideResolutionException extends RuntimeException {
        private final OperationError error;

        public TargetSideResolutionException(OperationError error) {
            super(Objects.requireNonNull(error, "error").errorMessage());
            this.error = error;
        }

        public OperationError error() {
            return error;
        }
    }
}
