package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

/**
 * Server-side registrar event used to register server runtime tool providers.
 */
public final class RegisterServerMcpToolsEvent extends RegisterMcpToolsEvent {

    public RegisterServerMcpToolsEvent(List<McpToolProvider> providers, ModMcpApi api, EventPublisher eventPublisher) {
        super(providers, api, eventPublisher);
    }
}
