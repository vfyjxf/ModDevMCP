package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.host.transport.RuntimeHost;

import java.io.IOException;

public final class ModDevMcpBackend implements AutoCloseable {

    private final RuntimeHost runtimeHost;
    private final BackendMcpProxyServer mcpProxyServer;

    private ModDevMcpBackend(RuntimeHost runtimeHost, BackendMcpProxyServer mcpProxyServer) {
        this.runtimeHost = runtimeHost;
        this.mcpProxyServer = mcpProxyServer;
    }

    public static ModDevMcpBackend start(ModDevMcpServer server, HostEndpointConfig runtimeConfig, BackendMcpEndpointConfig mcpConfig) throws IOException {
        return new ModDevMcpBackend(
                ModDevMcpServerFactory.startRuntimeHost(server, runtimeConfig),
                BackendMcpProxyServer.start(server, mcpConfig.host(), mcpConfig.port())
        );
    }

    public int runtimePort() {
        return runtimeHost.port();
    }

    public int mcpPort() {
        return mcpProxyServer.port();
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            mcpProxyServer.close();
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            runtimeHost.close();
        } catch (IOException exception) {
            if (failure == null) {
                failure = exception;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
