package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.host.transport.RuntimeHost;
import dev.vfyjxf.mcp.server.transport.McpServerTransport;

import java.io.IOException;

final class HostAttachedStdioMcpServerHost implements McpServerTransport, AutoCloseable {

    private final McpServerTransport stdioHost;
    private final RuntimeHost runtimeHost;

    HostAttachedStdioMcpServerHost(McpServerTransport stdioHost, RuntimeHost runtimeHost) {
        this.stdioHost = stdioHost;
        this.runtimeHost = runtimeHost;
    }

    @Override
    public void serve() {
        try {
            stdioHost.serve();
        } finally {
            try {
                runtimeHost.close();
            } catch (IOException exception) {
                throw new RuntimeException("Failed to close runtime host", exception);
            }
        }
    }

    @Override
    public void close() throws IOException {
        runtimeHost.close();
    }
}


