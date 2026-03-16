package dev.vfyjxf.mcp.api.registrar;

import dev.vfyjxf.mcp.api.event.RegisterServerMcpToolsEvent;

/**
 * Callback interface implemented by server-side registrars discovered by ModDevMCP.
 */
@FunctionalInterface
public interface ServerMcpToolRegistrar {

    /**
     * Contributes server-side tools through the supplied event.
     */
    void register(RegisterServerMcpToolsEvent event);
}
