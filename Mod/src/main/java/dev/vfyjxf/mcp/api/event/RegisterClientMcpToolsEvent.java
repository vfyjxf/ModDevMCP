package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

public final class RegisterClientMcpToolsEvent extends RegisterMcpToolsEvent {

    public RegisterClientMcpToolsEvent(List<McpToolProvider> providers) {
        super(providers);
    }
}
