package dev.vfyjxf.moddev.server.bootstrap;

import dev.vfyjxf.moddev.server.transport.McpServerTransport;
import dev.vfyjxf.moddev.server.transport.StdioMcpServerHost;

import java.io.InputStream;
import java.io.OutputStream;

public final class ModDevMcpGatewayMain {

    private ModDevMcpGatewayMain() {
    }

    public static void main(String[] args) throws Exception {
        var config = GatewayBootstrapConfig.loadResolved();
        new GatewayBootstrap().ensureBackendAvailable(config);
        try (var host = createHost(System.in, System.out, config)) {
            host.serve();
        }
    }

    public static CloseableMcpServerTransport createHost(InputStream input, OutputStream output, GatewayBootstrapConfig config) throws java.io.IOException {
        var handler = new BackendProxyJsonRpcHandler(config.host(), config.mcpPort());
        return new CloseableMcpServerTransport(new StdioMcpServerHost(input, output, handler), handler);
    }

    public record CloseableMcpServerTransport(McpServerTransport delegate, AutoCloseable closeable) implements McpServerTransport, AutoCloseable {

        @Override
        public void serve() {
            delegate.serve();
        }

        @Override
        public void close() throws Exception {
            closeable.close();
        }
    }
}

