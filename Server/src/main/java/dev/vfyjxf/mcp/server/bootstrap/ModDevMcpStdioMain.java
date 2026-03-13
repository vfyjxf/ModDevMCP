package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.transport.McpServerTransport;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

public final class ModDevMcpStdioMain {

    private ModDevMcpStdioMain() {
    }

    public static void main(String[] args) throws Exception {
        var shutdown = new CountDownLatch(1);
        var host = createHost(System.in, System.out);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (host instanceof AutoCloseable closeable) {
                    closeable.close();
                }
            } catch (Exception exception) {
                throw new RuntimeException("Failed to close stdio host", exception);
            } finally {
                shutdown.countDown();
            }
        }, "moddev-stdio-shutdown"));
        host.serve();
        shutdown.await();
    }

    public static McpServerTransport createHost(InputStream input, OutputStream output) {
        return createHost(input, output, HostEndpointConfig.loadResolved());
    }

    static McpServerTransport createHost(InputStream input, OutputStream output, HostEndpointConfig config) {
        return ModDevMcpServerFactory.createHostAttachedStdioHost(ModDevMcpServerFactory.createServer(), input, output, config);
    }
}



