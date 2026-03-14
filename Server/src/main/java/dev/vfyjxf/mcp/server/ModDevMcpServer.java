package dev.vfyjxf.mcp.server;

import dev.vfyjxf.mcp.server.api.McpResourceProvider;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.host.HostStatusToolProvider;
import dev.vfyjxf.mcp.server.host.RuntimeCallQueue;
import dev.vfyjxf.mcp.server.host.RuntimeRegistry;
import dev.vfyjxf.mcp.server.runtime.McpResourceRegistry;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

public class ModDevMcpServer {

    private final McpToolRegistry registry;
    private final McpResourceRegistry resourceRegistry;
    private final RuntimeRegistry runtimeRegistry;
    private final RuntimeCallQueue callScheduler;

    public ModDevMcpServer() {
        this(new McpToolRegistry(), new McpResourceRegistry(), new RuntimeRegistry());
    }

    public ModDevMcpServer(McpToolRegistry registry) {
        this(registry, new McpResourceRegistry(), new RuntimeRegistry());
    }

    public ModDevMcpServer(McpToolRegistry registry, McpResourceRegistry resourceRegistry) {
        this(registry, resourceRegistry, new RuntimeRegistry());
    }

    public ModDevMcpServer(McpToolRegistry registry, McpResourceRegistry resourceRegistry, RuntimeRegistry runtimeRegistry) {
        this(registry, resourceRegistry, runtimeRegistry, new RuntimeCallQueue(runtimeRegistry));
    }

    public ModDevMcpServer(McpToolRegistry registry, McpResourceRegistry resourceRegistry, RuntimeRegistry runtimeRegistry, RuntimeCallQueue callScheduler) {
        this.registry = registry;
        this.resourceRegistry = resourceRegistry;
        this.runtimeRegistry = runtimeRegistry;
        this.callScheduler = callScheduler;
        new HostStatusToolProvider(runtimeRegistry, callScheduler).register(registry);
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

    public RuntimeRegistry runtimeRegistry() {
        return runtimeRegistry;
    }

    public RuntimeCallQueue callScheduler() {
        return callScheduler;
    }
}

