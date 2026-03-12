package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.transport.McpServerTransport;

import java.io.InputStream;
import java.io.OutputStream;

public final class EmbeddedModDevMcpHost {

    private EmbeddedModDevMcpHost() {
    }

    public static McpServerTransport createStdioHost(ModDevMcpServer server, InputStream input, OutputStream output) {
        return ModDevMcpServerFactory.createStdioHost(server, input, output);
    }
}
