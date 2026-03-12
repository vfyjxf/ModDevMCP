package dev.vfyjxf.mcp.server.transport;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.bootstrap.ModDevMcpServerFactory;
import io.modelcontextprotocol.server.McpSyncServer;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SdkStdioMcpServerHost implements McpServerTransport {

    private final ModDevMcpServer server;
    private final InputStream input;
    private final OutputStream output;
    private final AtomicBoolean started = new AtomicBoolean();

    private volatile McpSyncServer sdkServer;

    public SdkStdioMcpServerHost(ModDevMcpServer server, InputStream input, OutputStream output) {
        this.server = server;
        this.input = input;
        this.output = output;
    }

    @Override
    public void serve() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        sdkServer = ModDevMcpServerFactory.createSdkServer(
                server,
                new CompatStdioServerTransportProvider(ModDevMcpServerFactory.JSON_MAPPER, input, output)
        );
    }

    McpSyncServer sdkServer() {
        return sdkServer;
    }
}
