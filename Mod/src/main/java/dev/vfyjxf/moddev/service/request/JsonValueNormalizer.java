package dev.vfyjxf.moddev.service.request;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonValueNormalizer {

    private JsonValueNormalizer() {
    }

    static Map<String, Object> freezeObject(Map<?, ?> source, String fieldName) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        var copy = new LinkedHashMap<String, Object>(source.size());
        for (var entry : source.entrySet()) {
            var key = requireKey(entry.getKey(), fieldName);
            copy.put(key, freezeValue(entry.getValue(), fieldName + "." + key));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static List<Object> freezeList(List<?> source, String fieldName) {
        if (source.isEmpty()) {
            return List.of();
        }
        var copy = new ArrayList<Object>(source.size());
        for (int i = 0; i < source.size(); i++) {
            copy.add(freezeValue(source.get(i), fieldName + "[" + i + "]"));
        }
        return Collections.unmodifiableList(copy);
    }

    private static Object freezeValue(Object value, String fieldName) {
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
                throw new IllegalArgumentException(fieldName + " uses an unsupported number type");
            }
            if (numberValue instanceof Double doubleValue && !Double.isFinite(doubleValue)) {
                throw new IllegalArgumentException(fieldName + " must be finite");
            }
            if (numberValue instanceof Float floatValue && !Float.isFinite(floatValue)) {
                throw new IllegalArgumentException(fieldName + " must be finite");
            }
            return value;
        }
        if (value instanceof Map<?, ?> mapValue) {
            return freezeObject(mapValue, fieldName);
        }
        if (value instanceof List<?> listValue) {
            return freezeList(listValue, fieldName);
        }
        throw new IllegalArgumentException(fieldName + " must be JSON-compatible");
    }

    private static String requireKey(Object key, String fieldName) {
        if (!(key instanceof String stringKey) || stringKey.isBlank()) {
            throw new IllegalArgumentException(fieldName + " keys must be non-blank strings");
        }
        return stringKey;
    }
}

