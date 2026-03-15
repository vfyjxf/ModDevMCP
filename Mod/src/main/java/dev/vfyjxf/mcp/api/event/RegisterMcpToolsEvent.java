package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

abstract class RegisterMcpToolsEvent {

    private final List<McpToolProvider> providers;

    protected RegisterMcpToolsEvent(List<McpToolProvider> providers) {
        this.providers = providers;
    }

    public final void register(McpToolProvider provider) {
        providers.add(provider);
    }
}
