package dev.vfyjxf.mcp.server.api;

import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

@FunctionalInterface
public interface McpToolProvider {

    void register(McpToolRegistry registry);
}
