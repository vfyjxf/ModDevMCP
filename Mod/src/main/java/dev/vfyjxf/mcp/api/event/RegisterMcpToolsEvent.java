package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

public final class RegisterMcpToolsEvent {

    private final List<McpToolProvider> providers;

    public RegisterMcpToolsEvent(List<McpToolProvider> providers) {
        this.providers = providers;
    }

    public void register(McpToolProvider provider) {
        providers.add(provider);
    }
}
