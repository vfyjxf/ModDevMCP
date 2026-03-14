package dev.vfyjxf.mcp.server.bootstrap;

import java.io.InputStream;
import java.io.OutputStream;

public final class ModDevMcpStdioMain {

    private ModDevMcpStdioMain() {
    }

    public static void main(String[] args) throws Exception {
        ModDevMcpGatewayMain.main(args);
    }

    public static ModDevMcpGatewayMain.CloseableMcpServerTransport createHost(InputStream input, OutputStream output) {
        return createHost(input, output, HostEndpointConfig.loadResolved());
    }

    static ModDevMcpGatewayMain.CloseableMcpServerTransport createHost(InputStream input, OutputStream output, HostEndpointConfig config) {
        try {
            var server = ModDevMcpServerFactory.createServer();
            var backend = ModDevMcpBackend.start(server, config, new BackendMcpEndpointConfig(config.host(), 0));
            var gatewayConfig = new GatewayBootstrapConfig(
                    config.host(),
                    backend.runtimePort(),
                    backend.mcpPort(),
                    "java",
                    java.nio.file.Path.of("backend.args"),
                    null,
                    java.time.Duration.ofSeconds(1),
                    java.time.Duration.ofMillis(10)
            );
            var host = ModDevMcpGatewayMain.createHost(input, output, gatewayConfig);
            return new ModDevMcpGatewayMain.CloseableMcpServerTransport(host, backend);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to start embedded backend", exception);
        }
    }
}



