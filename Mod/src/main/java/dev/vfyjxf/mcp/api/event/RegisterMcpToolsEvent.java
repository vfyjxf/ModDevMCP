package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

abstract class RegisterMcpToolsEvent {

    private final List<McpToolProvider> providers;
    private final ModMcpApi api;
    private final EventPublisher eventPublisher;

    protected RegisterMcpToolsEvent(List<McpToolProvider> providers, ModMcpApi api, EventPublisher eventPublisher) {
        this.providers = providers;
        this.api = api;
        this.eventPublisher = eventPublisher;
    }

    public final void register(McpToolProvider provider) {
        providers.add(provider);
    }

    public final void registerToolProvider(McpToolProvider provider) {
        register(provider);
    }

    public final ModMcpApi api() {
        return api;
    }

    public final EventPublisher eventPublisher() {
        return eventPublisher;
    }

    public final void publishEvent(EventEnvelope event) {
        eventPublisher.publish(event);
    }
}
