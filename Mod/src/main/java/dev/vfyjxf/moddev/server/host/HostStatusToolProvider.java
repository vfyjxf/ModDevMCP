package dev.vfyjxf.moddev.server.host;

import dev.vfyjxf.moddev.server.api.McpToolDefinition;
import dev.vfyjxf.moddev.server.api.McpToolProvider;
import dev.vfyjxf.moddev.server.api.ToolResult;

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
    public void register(dev.vfyjxf.moddev.server.runtime.McpToolRegistry registry) {
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
        var sessions = runtimeRegistry.listSessions();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("hostReady", true);
        payload.put("gameConnected", state.gameConnected());
        payload.put("gameConnecting", state.gameConnecting());
        payload.put("clientConnected", sessions.stream().anyMatch(session -> "client".equalsIgnoreCase(session.runtimeSide())));
        payload.put("serverConnected", sessions.stream().anyMatch(session -> "server".equalsIgnoreCase(session.runtimeSide())));
        payload.put("connectedAgentCount", 0);
        payload.put("queueDepth", callScheduler == null ? 0 : callScheduler.queueDepth());
        payload.put("runtimeId", state.runtimeId());
        payload.put("runtimeSide", state.runtimeSide());
        payload.put("availableScopes", sessions.stream().flatMap(session -> session.supportedScopes().stream()).distinct().toList());
        payload.put("runtimeSides", sessions.stream().map(RuntimeSession::runtimeSide).distinct().toList());
        payload.put("connectedRuntimes", sessions.stream().map(this::runtimePayload).toList());
        return Map.copyOf(payload);
    }

    private Map<String, Object> runtimePayload(RuntimeSession session) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("runtimeId", session.runtimeId());
        payload.put("runtimeSide", session.runtimeSide());
        payload.put("supportedScopes", session.supportedScopes());
        payload.put("supportedSides", session.supportedSides());
        payload.put("state", session.state());
        return Map.copyOf(payload);
    }
}

