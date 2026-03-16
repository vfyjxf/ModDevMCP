package dev.vfyjxf.mcp.api.registrar;

import dev.vfyjxf.mcp.api.event.RegisterClientMcpToolsEvent;

/**
 * Callback interface implemented by client-side registrars discovered by ModDevMCP.
 */
@FunctionalInterface
public interface ClientMcpToolRegistrar {

    /**
     * Contributes client-side tools or runtime adapters through the supplied event.
     */
    void register(RegisterClientMcpToolsEvent event);
}
