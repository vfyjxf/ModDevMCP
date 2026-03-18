package dev.vfyjxf.mcp.service.request;

public record OperationError(String errorCode, String errorMessage) {

    public OperationError {
        validate(errorCode, "errorCode");
        validate(errorMessage, "errorMessage");
    }

    private static void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not include leading or trailing whitespace");
        }
    }
}
