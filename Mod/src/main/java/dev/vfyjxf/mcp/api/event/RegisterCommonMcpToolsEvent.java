package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

public final class RegisterCommonMcpToolsEvent extends RegisterMcpToolsEvent {

    public RegisterCommonMcpToolsEvent(List<McpToolProvider> providers) {
        super(providers);
    }
}
