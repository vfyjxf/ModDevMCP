package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketBridgeProxyTest {

    @Test
    void proxyRelaysBidirectionalTrafficBetweenStdioAndBridgeSocket() throws Exception {
        try (var serverSocket = new ServerSocket(0)) {
            Future<byte[]> received = Executors.newSingleThreadExecutor().submit(() -> {
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

            try (var socket = new Socket("127.0.0.1", serverSocket.getLocalPort())) {
                SocketBridgeProxy.proxy(stdin, stdout, socket);
            }

            assertEquals("""
                    {"jsonrpc":"2.0","id":1,"method":"initialize"}
                    """.strip(), new String(received.get(), StandardCharsets.UTF_8).strip());
            assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("\"ok\":true"));
        }
    }

    @Test
    void proxyFlushesStdoutBeforeSocketCloses() throws Exception {
        try (var serverSocket = new ServerSocket(0)) {
            CountDownLatch responseWritten = new CountDownLatch(1);
            CountDownLatch allowClose = new CountDownLatch(1);
            Future<?> server = Executors.newSingleThreadExecutor().submit(() -> {
                try (var accepted = serverSocket.accept()) {
                    accepted.getInputStream().readAllBytes();
                    accepted.getOutputStream().write("""
                            {"jsonrpc":"2.0","id":1,"result":{"ok":true}}
                            """.getBytes(StandardCharsets.UTF_8));
                    accepted.getOutputStream().flush();
                    responseWritten.countDown();
                    assertTrue(allowClose.await(5, TimeUnit.SECONDS));
                    accepted.shutdownOutput();
                    return null;
                }
            });

            var stdin = new ByteArrayInputStream("""
                    {"jsonrpc":"2.0","id":1,"method":"initialize"}
                    """.getBytes(StandardCharsets.UTF_8));
            var stdout = new FlushTrackingOutputStream();
            Future<?> proxy = Executors.newSingleThreadExecutor().submit(() -> {
                try (var socket = new Socket("127.0.0.1", serverSocket.getLocalPort())) {
                    SocketBridgeProxy.proxy(stdin, stdout, socket);
                }
                return null;
            });

            assertTrue(responseWritten.await(5, TimeUnit.SECONDS));
            Thread.sleep(200L);
            assertTrue(stdout.visibleString().contains("\"ok\":true"));

            allowClose.countDown();
            proxy.get(5, TimeUnit.SECONDS);
            server.get(5, TimeUnit.SECONDS);
        }
    }

    private static final class FlushTrackingOutputStream extends OutputStream {
        private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
        private final ByteArrayOutputStream visible = new ByteArrayOutputStream();

        @Override
        public synchronized void write(int b) {
            pending.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            pending.write(b, off, len);
        }

        @Override
        public synchronized void flush() {
            try {
                pending.writeTo(visible);
            } catch (java.io.IOException exception) {
                throw new RuntimeException(exception);
            }
            pending.reset();
        }

        synchronized String visibleString() {
            return visible.toString(StandardCharsets.UTF_8);
        }
    }
}
