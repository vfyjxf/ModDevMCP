package dev.vfyjxf.mcp.server.runtime;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolHandler;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class McpToolRegistry {

    private final Map<String, RegisteredTool> tools = new LinkedHashMap<>();

    public void registerProvider(McpToolProvider provider) {
        provider.register(this);
    }

    public void registerTool(McpToolDefinition definition, McpToolHandler handler) {
        var tool = new RegisteredTool(definition, handler);
        if (tools.putIfAbsent(definition.name(), tool) != null) {
            throw new IllegalArgumentException("Duplicate tool: " + definition.name());
        }
    }

    public Optional<RegisteredTool> findTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<RegisteredTool> listTools() {
        return tools.values();
    }

    public record RegisteredTool(
            McpToolDefinition definition,
            McpToolHandler handler
    ) {
    }
}
