package dev.vfyjxf.mcp.service.request;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record OperationRequest(
        String requestId,
        String operationId,
        String targetSide,
        Map<String, Object> input
) {
    private static final Set<String> ALLOWED_KEYS = Set.of("requestId", "operationId", "targetSide", "input");

    public OperationRequest {
        requestId = validateOptionalString(requestId, "requestId");
        operationId = validateRequiredString(operationId, "operationId");
        targetSide = validateOptionalString(targetSide, "targetSide");
        input = freezeInput(input);
    }

    public static OperationRequest fromPayload(Map<String, Object> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        for (var key : payload.keySet()) {
            if (!ALLOWED_KEYS.contains(key)) {
                throw new IllegalArgumentException("unsupported request field: " + key);
            }
        }

        var requestId = readOptionalString(payload, "requestId");
        var operationId = readRequiredString(payload, "operationId");
        var targetSide = readOptionalString(payload, "targetSide");
        var input = readInput(payload.get("input"));
        return new OperationRequest(requestId, operationId, targetSide, input);
    }

    private static String readRequiredString(Map<String, Object> payload, String fieldName) {
        var value = payload.get(fieldName);
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException(fieldName + " must be a string");
        }
        return stringValue;
    }

    private static String readOptionalString(Map<String, Object> payload, String fieldName) {
        var value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException(fieldName + " must be a string");
        }
        return stringValue;
    }

    private static Map<String, Object> readInput(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> mapValue)) {
            throw new IllegalArgumentException("input must be an object");
        }
        var result = new LinkedHashMap<String, Object>();
        for (var entry : mapValue.entrySet()) {
            if (!(entry.getKey() instanceof String key) || key.isBlank()) {
                throw new IllegalArgumentException("input keys must be non-blank strings");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static String validateRequiredString(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not include leading or trailing whitespace");
        }
        return value;
    }

    private static String validateOptionalString(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not include leading or trailing whitespace");
        }
        return value;
    }

    private static Map<String, Object> freezeInput(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(input);
    }
}
