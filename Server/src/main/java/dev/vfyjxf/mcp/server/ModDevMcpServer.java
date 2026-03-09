package dev.vfyjxf.mcp.server;

import dev.vfyjxf.mcp.server.api.McpResourceProvider;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.runtime.McpResourceRegistry;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

public class ModDevMcpServer {

    private final McpToolRegistry registry;
    private final McpResourceRegistry resourceRegistry;

    public ModDevMcpServer() {
        this(new McpToolRegistry(), new McpResourceRegistry());
    }

    public ModDevMcpServer(McpToolRegistry registry) {
        this(registry, new McpResourceRegistry());
    }

    public ModDevMcpServer(McpToolRegistry registry, McpResourceRegistry resourceRegistry) {
        this.registry = registry;
        this.resourceRegistry = resourceRegistry;
    }

    public void registerProvider(McpToolProvider provider) {
        registry.registerProvider(provider);
    }

    public void registerResourceProvider(McpResourceProvider provider) {
        resourceRegistry.registerProvider(provider);
    }

    public McpToolRegistry registry() {
        return registry;
    }

    public McpResourceRegistry resourceRegistry() {
        return resourceRegistry;
    }
}
