package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
}
