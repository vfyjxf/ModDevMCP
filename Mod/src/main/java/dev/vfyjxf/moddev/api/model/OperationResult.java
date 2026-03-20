package dev.vfyjxf.moddev.api.model;

public record OperationResult<T>(
        boolean accepted,
        boolean performed,
        String reason,
        T value,
        String snapshotRef
) {
    public static <T> OperationResult<T> success(T value) {
        return new OperationResult<>(true, true, null, value, null);
    }

    public static <T> OperationResult<T> rejected(String reason) {
        return new OperationResult<>(false, false, reason, null, null);
    }
}

