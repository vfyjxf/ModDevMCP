package dev.vfyjxf.mcp.server.host.protocol;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.host.RuntimeRegistry;
import dev.vfyjxf.mcp.server.host.RuntimeSession;
import dev.vfyjxf.mcp.server.host.RuntimeToolDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RuntimeHostDispatcher {

    private final RuntimeRegistry registry;

    public RuntimeHostDispatcher(RuntimeRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public Map<String, Object> handle(Map<String, Object> request) {
        var type = requiredString(request, "type");
        return switch (type) {
            case "runtime.hello" -> handleHello(request);
            case "runtime.refresh" -> handleRefresh(request);
            case "runtime.goodbye" -> handleGoodbye(request);
            default -> error("unsupported_type", type);
        };
    }

    private Map<String, Object> handleHello(Map<String, Object> request) {
        var runtimeId = requiredString(request, "runtimeId");
        var runtimeSide = requiredString(request, "runtimeSide");
        var supportedScopes = stringList(request.get("supportedScopes"));
        var supportedSides = stringList(request.get("supportedSides"));
        var toolDescriptors = toolDescriptors(request.get("toolDescriptors"));
        var state = objectMap(request.getOrDefault("state", Map.of()));
        registry.connect(new RuntimeSession(runtimeId, runtimeSide, supportedScopes, supportedSides, state), toolDescriptors);
        return Map.of(
                "status", "ok",
                "runtimeId", runtimeId,
                "toolCount", toolDescriptors.size()
        );
    }

    private Map<String, Object> handleRefresh(Map<String, Object> request) {
        var runtimeId = requiredString(request, "runtimeId");
        var toolDescriptors = toolDescriptors(request.get("toolDescriptors"));
        registry.refreshTools(runtimeId, toolDescriptors);
        return Map.of(
                "status", "ok",
                "runtimeId", runtimeId,
                "toolCount", toolDescriptors.size()
        );
    }

    private Map<String, Object> handleGoodbye(Map<String, Object> request) {
        var runtimeId = requiredString(request, "runtimeId");
        registry.disconnect(runtimeId);
        return Map.of(
                "status", "ok",
                "runtimeId", runtimeId
        );
    }

    private List<RuntimeToolDescriptor> toolDescriptors(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("toolDescriptors must be a list");
        }
        return list.stream()
                .map(this::toolDescriptor)
                .toList();
    }

    private RuntimeToolDescriptor toolDescriptor(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("toolDescriptor must be an object");
        }
        var map = objectMap(rawMap);
        return new RuntimeToolDescriptor(
                new McpToolDefinition(
                        requiredString(map, "name"),
                        requiredString(map, "title"),
                        requiredString(map, "description"),
                        objectMap(map.getOrDefault("inputSchema", Map.of("type", "object"))),
                        objectMap(map.getOrDefault("outputSchema", Map.of("type", "object"))),
                        stringList(map.getOrDefault("tags", List.of())),
                        requiredString(map, "side"),
                        requiredBoolean(map, "requiresWorld"),
                        requiredBoolean(map, "requiresPlayer"),
                        requiredString(map, "availability"),
                        requiredString(map, "exposurePolicy")
                ),
                requiredString(map, "scope"),
                requiredString(map, "runtimeToolSide"),
                requiredBoolean(map, "requiresGame"),
                requiredBoolean(map, "mutating")
        );
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Expected list value");
        }
        return list.stream().map(String::valueOf).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Expected object value");
        }
        return (Map<String, Object>) map;
    }

    private String requiredString(Map<String, Object> request, String key) {
        var value = request.get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException(key + " must be a non-empty string");
        }
        return stringValue;
    }

    private boolean requiredBoolean(Map<String, Object> request, String key) {
        var value = request.get(key);
        if (!(value instanceof Boolean booleanValue)) {
            throw new IllegalArgumentException(key + " must be a boolean");
        }
        return booleanValue;
    }

    private Map<String, Object> error(String code, String message) {
        return Map.of(
                "status", "error",
                "code", code,
                "message", message
        );
    }
}

