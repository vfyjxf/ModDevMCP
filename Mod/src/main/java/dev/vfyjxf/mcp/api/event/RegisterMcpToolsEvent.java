package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

/**
 * Base event passed to registrar callbacks while ModDevMCP is collecting tool providers.
 *
 * <p>Subclasses expose side-specific registration helpers while this base type keeps the common
 * provider, API, and event-publishing surface.
 */
abstract class RegisterMcpToolsEvent {

    private final List<McpToolProvider> providers;
    private final ModMcpApi api;
    private final EventPublisher eventPublisher;

    protected RegisterMcpToolsEvent(List<McpToolProvider> providers, ModMcpApi api, EventPublisher eventPublisher) {
        this.providers = providers;
        this.api = api;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Adds a tool provider to the current registration batch.
     */
    public final void register(McpToolProvider provider) {
        providers.add(provider);
    }

    /**
     * Adds a tool provider to the current registration batch.
     */
    public final void registerToolProvider(McpToolProvider provider) {
        register(provider);
    }

    /**
     * Returns the public runtime registration API associated with this callback.
     */
    public final ModMcpApi api() {
        return api;
    }

    /**
     * Returns the shared event publisher associated with the current runtime.
     */
    public final EventPublisher eventPublisher() {
        return eventPublisher;
    }

    /**
     * Publishes a runtime event immediately through the shared event publisher.
     */
    public final void publishEvent(EventEnvelope event) {
        eventPublisher.publish(event);
    }
}
