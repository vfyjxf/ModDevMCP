package dev.vfyjxf.moddev.server.bootstrap;

import java.util.concurrent.CountDownLatch;

public final class ModDevMcpBackendMain {

    private ModDevMcpBackendMain() {
    }

    public static void main(String[] args) throws Exception {
        var runtimeConfig = HostEndpointConfig.loadResolved();
        var mcpConfig = BackendMcpEndpointConfig.loadResolved();
        var server = ModDevMcpServerFactory.createServer();
        var shutdown = new CountDownLatch(1);
        try (var backend = ModDevMcpBackend.start(server, runtimeConfig, mcpConfig)) {
            Runtime.getRuntime().addShutdownHook(new Thread(shutdown::countDown, "moddev-backend-shutdown"));
            System.out.println("moddev backend ready runtime=" + runtimeConfig.host() + ":" + backend.runtimePort()
                    + " mcp=" + mcpConfig.host() + ":" + backend.mcpPort());
            shutdown.await();
        }
    }
}

