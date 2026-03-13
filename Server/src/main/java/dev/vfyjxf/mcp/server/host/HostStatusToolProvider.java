package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HostStatusToolProvider implements McpToolProvider {

    private final RuntimeRegistry runtimeRegistry;
    private final RuntimeCallQueue callScheduler;

    public HostStatusToolProvider(RuntimeRegistry runtimeRegistry) {
        this(runtimeRegistry, null);
    }

    public HostStatusToolProvider(RuntimeRegistry runtimeRegistry, RuntimeCallQueue callScheduler) {
        this.runtimeRegistry = runtimeRegistry;
        this.callScheduler = callScheduler;
    }

    @Override
    public void register(dev.vfyjxf.mcp.server.runtime.McpToolRegistry registry) {
        registry.registerTool(
                new McpToolDefinition(
                        "moddev.status",
                        "Host Status",
                        "Reports host and game runtime status",
                        Map.of("type", "object"),
                        Map.of("type", "object"),
                        List.of("status", "host"),
                        "either",
                        false,
                        false,
                        "public",
                        "public"
                ),
                (context, arguments) -> ToolResult.success(statusPayload())
        );
    }

    private Map<String, Object> statusPayload() {
        var state = runtimeRegistry.state();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("hostReady", true);
        payload.put("gameConnected", state.gameConnected());
        payload.put("gameConnecting", state.gameConnecting());
        payload.put("connectedAgentCount", 0);
        payload.put("queueDepth", callScheduler == null ? 0 : callScheduler.queueDepth());
        payload.put("runtimeId", state.runtimeId());
        payload.put("runtimeSide", state.runtimeSide());
        payload.put("availableScopes", runtimeRegistry.activeSession().map(RuntimeSession::supportedScopes).orElse(List.of()));
        payload.put("runtimeSides", runtimeRegistry.activeSession().map(RuntimeSession::supportedSides).orElse(List.of()));
        return Map.copyOf(payload);
    }
}


