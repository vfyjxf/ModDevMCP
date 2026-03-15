package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

public final class RegisterCommonMcpToolsEvent extends RegisterMcpToolsEvent {

    public RegisterCommonMcpToolsEvent(List<McpToolProvider> providers, ModMcpApi api, EventPublisher eventPublisher) {
        super(providers, api, eventPublisher);
    }
}
