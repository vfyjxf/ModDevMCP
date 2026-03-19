package dev.vfyjxf.moddev.server.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendAvailabilityProbeTest {

    @Test
    void probeReturnsFalseWhenBackendPortIsClosed() {
        var probe = new BackendAvailabilityProbe(Duration.ofMillis(250));

        assertFalse(probe.isReady("127.0.0.1", freePort()));
    }

    @Test
    void probeReturnsTrueWhenBackendAnswersInitialize() throws Exception {
        var port = freePort();
        var ready = new CountDownLatch(1);
        var serverThread = new Thread(() -> serveInitialize(port, ready), "backend-availability-probe-test");
        serverThread.setDaemon(true);
        serverThread.start();
        ready.await();

        var probe = new BackendAvailabilityProbe(Duration.ofSeconds(1));
        assertTrue(probe.isReady("127.0.0.1", port));
    }

    private void serveInitialize(int port, CountDownLatch ready) {
        try (var serverSocket = new ServerSocket(port)) {
            ready.countDown();
            try (var socket = serverSocket.accept();
                 var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                var line = reader.readLine();
                if (line != null && line.contains("\"method\":\"initialize\"")) {
                    writer.write("""
                            {"jsonrpc":"2.0","id":"probe","result":{"serverInfo":{"name":"moddev-mcp","version":"0.1.0"},"protocolVersion":"2025-06-18","capabilities":{"tools":{"listChanged":true}}}}
                            """.strip());
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private int freePort() {
        try (var serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

