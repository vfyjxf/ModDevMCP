package dev.vfyjxf.mcp.api.registrar;

import dev.vfyjxf.mcp.api.event.RegisterServerMcpToolsEvent;

@FunctionalInterface
public interface ServerMcpToolRegistrar {

    void register(RegisterServerMcpToolsEvent event);
}
