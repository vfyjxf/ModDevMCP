package dev.vfyjxf.mcp.server.bootstrap;

import java.util.concurrent.CountDownLatch;

public final class RuntimeHostMain {

    private RuntimeHostMain() {
    }

    public static void main(String[] args) throws Exception {
        var config = HostEndpointConfig.loadResolved();
        var server = ModDevMcpServerFactory.createServer();
        var shutdown = new CountDownLatch(1);
        try (var runtimeHost = ModDevMcpServerFactory.startRuntimeHost(server, config)) {
            Runtime.getRuntime().addShutdownHook(new Thread(shutdown::countDown, "moddev-host-server-shutdown"));
            System.out.println("moddev host runtime listener ready on " + config.host() + ":" + runtimeHost.port());
            shutdown.await();
        }
    }
}



