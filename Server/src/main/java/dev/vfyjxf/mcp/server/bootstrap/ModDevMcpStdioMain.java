package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.transport.McpServerTransport;

import java.io.InputStream;
import java.io.OutputStream;

public final class ModDevMcpStdioMain {

    private ModDevMcpStdioMain() {
    }

    public static void main(String[] args) {
        createHost(System.in, System.out).serve();
    }

    public static McpServerTransport createHost(InputStream input, OutputStream output) {
        return ModDevMcpServerFactory.createStdioHost(ModDevMcpServerFactory.createServer(), input, output);
    }
}
