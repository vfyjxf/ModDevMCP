package dev.vfyjxf.mcp.api.registrar;

import dev.vfyjxf.mcp.api.event.RegisterCommonMcpToolsEvent;

/**
 * Callback interface implemented by common-side registrars discovered by ModDevMCP.
 */
@FunctionalInterface
public interface CommonMcpToolRegistrar {

    /**
     * Contributes side-neutral tools through the supplied event.
     */
    void register(RegisterCommonMcpToolsEvent event);
}
