package dev.vfyjxf.mcp.api.registrar;

import dev.vfyjxf.mcp.api.event.RegisterCommonMcpToolsEvent;

@FunctionalInterface
public interface CommonMcpToolRegistrar {

    void register(RegisterCommonMcpToolsEvent event);
}
