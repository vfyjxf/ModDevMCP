package dev.vfyjxf.mcp.bootstrap;

import dev.vfyjxf.mcp.server.bootstrap.EmbeddedGameMcpConfig;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMcpBridgeMainTest {

    @Test
    void bridgeConnectsToExistingGameSocketAndProxiesTraffic() throws Exception {
        try (var serverSocket = new ServerSocket(0);
             var executor = Executors.newSingleThreadExecutor()) {
            var receivedFuture = executor.submit(() -> {
                try (var accepted = serverSocket.accept()) {
                    var inbound = accepted.getInputStream().readAllBytes();
                    accepted.getOutputStream().write("""
                            {"jsonrpc":"2.0","id":1,"result":{"ok":true}}
                            """.getBytes(StandardCharsets.UTF_8));
                    accepted.getOutputStream().flush();
                    accepted.shutdownOutput();
                    return inbound;
                }
            });

            var stdin = new ByteArrayInputStream("""
                    {"jsonrpc":"2.0","id":1,"method":"initialize"}
                    """.getBytes(StandardCharsets.UTF_8));
            var stdout = new ByteArrayOutputStream();

            GameMcpBridgeMain.run(
                    stdin,
                    stdout,
                    new EmbeddedGameMcpConfig("127.0.0.1", serverSocket.getLocalPort())
            );

            assertEquals("""
                    {"jsonrpc":"2.0","id":1,"method":"initialize"}
                    """.strip(), new String(receivedFuture.get(5, TimeUnit.SECONDS), StandardCharsets.UTF_8).strip());
            assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("\"ok\":true"));
        }
    }
}
