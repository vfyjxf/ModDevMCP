package dev.vfyjxf.mcp.bootstrap;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.bootstrap.EmbeddedModDevMcpHost;
import dev.vfyjxf.mcp.server.transport.McpServerTransport;

import java.io.InputStream;
import java.io.OutputStream;

public final class EmbeddedModDevMcpStdioMain {

    private EmbeddedModDevMcpStdioMain() {
    }

    public static void main(String[] args) {
        createHost(System.in, System.out).serve();
    }

    public static McpServerTransport createHost(InputStream input, OutputStream output) {
        var mod = new ModDevMCP();
        return EmbeddedModDevMcpHost.createStdioHost(mod.prepareServer(), input, output);
    }
}
