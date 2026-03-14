package dev.vfyjxf.mcp.runtime.host;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.List;
import java.util.Map;

public final class GameRuntimeDescriptorFactory {

    private final ModDevMcpServer server;
    private final String runtimeSide;

    public GameRuntimeDescriptorFactory(ModDevMcpServer server, String runtimeSide) {
        this.server = server;
        this.runtimeSide = runtimeSide;
    }

    public List<Map<String, Object>> createToolDescriptors() {
        return server.registry().listTools().stream()
                .map(McpToolRegistry.RegisteredTool::definition)
                .filter(definition -> !"moddev.status".equals(definition.name()))
                .map(definition -> Map.<String, Object>ofEntries(
                        Map.entry("name", definition.name()),
                        Map.entry("title", definition.title()),
                        Map.entry("description", definition.description()),
                        Map.entry("inputSchema", definition.inputSchema()),
                        Map.entry("outputSchema", definition.outputSchema()),
                        Map.entry("tags", definition.tags()),
                        Map.entry("side", definition.side()),
                        Map.entry("requiresWorld", definition.requiresWorld()),
                        Map.entry("requiresPlayer", definition.requiresPlayer()),
                        Map.entry("availability", "runtime"),
                        Map.entry("exposurePolicy", "runtime"),
                        Map.entry("scope", scopeFor(definition.side())),
                        Map.entry("runtimeToolSide", runtimeSide),
                        Map.entry("requiresGame", true),
                        Map.entry("mutating", isMutating(definition.name()))
                ))
                .toList();
    }

    private String scopeFor(String side) {
        return "client".equalsIgnoreCase(side) ? "client" : "common";
    }

    private boolean isMutating(String toolName) {
        return toolName.contains("action")
                || toolName.contains("click")
                || toolName.contains("hover")
                || toolName.contains("press")
                || toolName.contains("type")
                || toolName.contains("open")
                || toolName.contains("close")
                || toolName.contains("switch")
                || toolName.contains("compile")
                || toolName.contains("hotswap");
    }
}

