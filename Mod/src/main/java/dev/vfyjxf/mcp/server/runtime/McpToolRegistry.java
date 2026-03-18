package dev.vfyjxf.mcp.server.runtime;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolHandler;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class McpToolRegistry {

    private final Map<String, Map<String, RegisteredTool>> tools = new LinkedHashMap<>();

    public void registerProvider(McpToolProvider provider) {
        provider.register(this);
    }

    public void registerTool(McpToolDefinition definition, McpToolHandler handler) {
        var tool = new RegisteredTool(definition, handler);
        var side = normalizeSide(definition.side());
        var variants = tools.computeIfAbsent(definition.name(), ignored -> new LinkedHashMap<>());
        if (variants.putIfAbsent(side, tool) != null) {
            throw new IllegalArgumentException("Duplicate tool: " + definition.name());
        }
    }

    public Optional<RegisteredTool> findTool(String name, String requestedSide) {
        var variants = tools.get(name);
        if (variants == null || variants.isEmpty()) {
            return Optional.empty();
        }
        var side = normalizeSide(requestedSide);
        if (!side.isEmpty()) {
            var exact = variants.get(side);
            if (exact != null) {
                return Optional.of(exact);
            }
        }
        var either = variants.get("either");
        if (either != null) {
            return Optional.of(either);
        }
        var common = variants.get("common");
        if (common != null) {
            return Optional.of(common);
        }
        if (variants.size() == 1) {
            return Optional.of(variants.values().iterator().next());
        }
        return Optional.empty();
    }

    public Optional<RegisteredTool> findTool(String name) {
        return findTool(name, null);
    }

    public Collection<RegisteredTool> listTools() {
        var all = new ArrayList<RegisteredTool>();
        tools.values().forEach(variants -> all.addAll(variants.values()));
        return java.util.List.copyOf(all);
    }

    private static String normalizeSide(String side) {
        if (side == null) {
            return "";
        }
        return side.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record RegisteredTool(
            McpToolDefinition definition,
            McpToolHandler handler
    ) {
    }
}
