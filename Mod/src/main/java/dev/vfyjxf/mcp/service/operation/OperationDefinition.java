package dev.vfyjxf.mcp.service.operation;

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
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }

        availableTargetSides = freezeSet(availableTargetSides);
        inputSchema = freezeMap(inputSchema);
        validateExampleRequestKeys(exampleRequest);
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
            copy.put(entry.getKey(), freezeValue(entry.getValue()));
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
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    private static Set<Object> freezeGenericSet(Set<?> source) {
        if (source.isEmpty()) {
            return Set.of();
        }
        var copy = new LinkedHashSet<Object>();
        for (var value : source) {
            copy.add(freezeValue(value));
        }
        return Collections.unmodifiableSet(copy);
    }

    private static List<Object> freezeList(List<?> source) {
        if (source.isEmpty()) {
            return List.of();
        }
        var copy = new ArrayList<Object>(source.size());
        for (var item : source) {
            copy.add(freezeValue(item));
        }
        return List.copyOf(copy);
    }

    private static Object freezeValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            var nested = new LinkedHashMap<String, Object>();
            for (var entry : mapValue.entrySet()) {
                nested.put(String.valueOf(entry.getKey()), freezeValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(nested);
        }
        if (value instanceof List<?> listValue) {
            return freezeList(listValue);
        }
        if (value instanceof Set<?> setValue) {
            return freezeGenericSet(setValue);
        }
        return value;
    }

    private static void validateExampleRequestKeys(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (var key : source.keySet()) {
            if (!ALLOWED_EXAMPLE_REQUEST_KEYS.contains(key)) {
                throw new IllegalArgumentException("exampleRequest contains unsupported key: " + key);
            }
        }
    }
}
