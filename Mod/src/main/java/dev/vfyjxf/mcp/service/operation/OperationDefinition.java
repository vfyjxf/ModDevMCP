package dev.vfyjxf.mcp.service.operation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.LinkedHashSet;

public record OperationDefinition(
        String operationId,
        String categoryId,
        String title,
        String summary,
        boolean supportsTargetSide,
        Set<String> availableTargetSides,
        Map<String, Object> inputSchema,
        Map<String, Object> exampleRequest
) {
    private static final Set<String> ALLOWED_EXAMPLE_REQUEST_KEYS = Set.of("requestId", "operationId", "targetSide", "input");
    private static final Set<String> ALLOWED_TARGET_SIDES = Set.of("client", "server");

    public OperationDefinition {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        ensureNoPadding(operationId, "operationId");
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        ensureNoPadding(categoryId, "categoryId");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }

        availableTargetSides = freezeSet(availableTargetSides);
        inputSchema = freezeMap(inputSchema);
        validateExampleRequestKeys(exampleRequest);
        validateExampleRequestShape(exampleRequest, operationId, supportsTargetSide, availableTargetSides);
        exampleRequest = freezeMap(exampleRequest);

        if (supportsTargetSide && availableTargetSides.isEmpty()) {
            throw new IllegalArgumentException("availableTargetSides must not be empty when supportsTargetSide is true");
        }
        if (!supportsTargetSide && !availableTargetSides.isEmpty()) {
            throw new IllegalArgumentException("availableTargetSides must be empty when supportsTargetSide is false");
        }
    }

    private static Map<String, Object> freezeMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        var copy = new LinkedHashMap<String, Object>();
        for (var entry : source.entrySet()) {
            var key = validateMapKey(entry.getKey());
            copy.put(key, freezeValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Set<String> freezeSet(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        for (var side : source) {
            if (side == null || side.isBlank()) {
                throw new IllegalArgumentException("availableTargetSides must not contain null or blank members");
            }
            if (!ALLOWED_TARGET_SIDES.contains(side)) {
                throw new IllegalArgumentException("availableTargetSides contains unsupported value: " + side);
            }
        }
        var ordered = new LinkedHashSet<String>();
        for (var side : List.of("client", "server")) {
            if (source.contains(side)) {
                ordered.add(side);
            }
        }
        return Collections.unmodifiableSet(ordered);
    }

    private static List<Object> freezeList(List<?> source) {
        if (source.isEmpty()) {
            return List.of();
        }
        var copy = new ArrayList<Object>(source.size());
        for (var item : source) {
            copy.add(freezeValue(item));
        }
        return Collections.unmodifiableList(copy);
    }

    private static Object freezeValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Number numberValue) {
            if (!(numberValue instanceof Byte
                    || numberValue instanceof Short
                    || numberValue instanceof Integer
                    || numberValue instanceof Long
                    || numberValue instanceof Float
                    || numberValue instanceof Double
                    || numberValue instanceof BigInteger
                    || numberValue instanceof BigDecimal)) {
                throw new IllegalArgumentException("metadata numeric values must use immutable JSON-friendly number types");
            }
            if (numberValue instanceof Double doubleValue && !Double.isFinite(doubleValue)) {
                throw new IllegalArgumentException("metadata numeric values must be finite");
            }
            if (numberValue instanceof Float floatValue && !Float.isFinite(floatValue)) {
                throw new IllegalArgumentException("metadata numeric values must be finite");
            }
            return value;
        }
        if (value instanceof Map<?, ?> mapValue) {
            var nested = new LinkedHashMap<String, Object>();
            for (var entry : mapValue.entrySet()) {
                var key = validateMapKey(entry.getKey());
                nested.put(key, freezeValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(nested);
        }
        if (value instanceof List<?> listValue) {
            return freezeList(listValue);
        }
        if (value instanceof Set<?>) {
            throw new IllegalArgumentException("metadata values must not contain sets");
        }
        throw new IllegalArgumentException("metadata values must be JSON-safe primitives, maps, lists, or null");
    }

    private static String validateMapKey(Object key) {
        if (!(key instanceof String stringKey) || stringKey.isBlank()) {
            throw new IllegalArgumentException("metadata map keys must be non-null, non-blank strings");
        }
        return stringKey;
    }

    private static void validateExampleRequestKeys(Map<?, ?> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (var rawKey : source.keySet()) {
            var key = validateMapKey(rawKey);
            if (!ALLOWED_EXAMPLE_REQUEST_KEYS.contains(key)) {
                throw new IllegalArgumentException("exampleRequest contains unsupported key: " + key);
            }
        }
    }

    private static void validateExampleRequestShape(
            Map<String, Object> source,
            String operationId,
            boolean supportsTargetSide,
            Set<String> availableTargetSides
    ) {
        if (source == null || source.isEmpty()) {
            return;
        }
        if (!source.containsKey("operationId")) {
            throw new IllegalArgumentException("exampleRequest.operationId is required when exampleRequest is non-empty");
        }

        if (source.containsKey("operationId")) {
            var operationIdValue = source.get("operationId");
            if (!(operationIdValue instanceof String operationIdString)) {
                throw new IllegalArgumentException("exampleRequest.operationId must be a string");
            }
            if (!operationId.equals(operationIdString)) {
                throw new IllegalArgumentException("exampleRequest.operationId must match definition operationId");
            }
        }

        if (source.containsKey("targetSide")) {
            if (!supportsTargetSide) {
                throw new IllegalArgumentException("exampleRequest.targetSide must be absent when supportsTargetSide is false");
            }
            var targetSideValue = source.get("targetSide");
            if (!(targetSideValue instanceof String targetSideString)) {
                throw new IllegalArgumentException("exampleRequest.targetSide must be a string");
            }
            if (!availableTargetSides.contains(targetSideString)) {
                throw new IllegalArgumentException("exampleRequest.targetSide must be one of availableTargetSides");
            }
        }

        if (source.containsKey("requestId")) {
            var requestIdValue = source.get("requestId");
            if (!(requestIdValue instanceof String requestIdString) || requestIdString.isBlank()) {
                throw new IllegalArgumentException("exampleRequest.requestId must be a non-blank string");
            }
        }

        if (source.containsKey("input")) {
            var inputValue = source.get("input");
            if (!(inputValue instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("exampleRequest.input must be an object");
            }
        }
    }

    private static void ensureNoPadding(String value, String fieldName) {
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not include leading or trailing whitespace");
        }
    }
}
