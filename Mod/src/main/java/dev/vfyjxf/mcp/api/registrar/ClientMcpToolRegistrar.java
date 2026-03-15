package dev.vfyjxf.mcp.api.registrar;

import dev.vfyjxf.mcp.api.event.RegisterClientMcpToolsEvent;

@FunctionalInterface
public interface ClientMcpToolRegistrar {

    void register(RegisterClientMcpToolsEvent event);
}
