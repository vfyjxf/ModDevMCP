package dev.vfyjxf.moddev.server.api;

import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;

@FunctionalInterface
public interface McpToolProvider {

    void register(McpToolRegistry registry);
}

