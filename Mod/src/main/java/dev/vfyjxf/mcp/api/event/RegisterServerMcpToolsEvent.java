package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

public final class RegisterServerMcpToolsEvent extends RegisterMcpToolsEvent {

    public RegisterServerMcpToolsEvent(List<McpToolProvider> providers) {
        super(providers);
    }
}
