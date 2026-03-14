package dev.vfyjxf.mcp.server.host.transport;

import dev.vfyjxf.mcp.server.host.RuntimeRegistry;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeHostTest {

    @Test
    void hostAcceptsRuntimeHelloAndRegistersSession() throws Exception {
        var registry = new RuntimeRegistry();
        try (var host = RuntimeHost.start(registry, "127.0.0.1", freePort())) {
            try (var socket = new Socket("127.0.0.1", host.port());
                 var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write("{\"type\":\"runtime.hello\",\"runtimeId\":\"runtime-1\",\"runtimeSide\":\"client\",\"supportedScopes\":[\"common\",\"client\"],\"supportedSides\":[\"client\"],\"toolDescriptors\":[],\"state\":{}}\n");
                writer.flush();
                waitUntil(() -> registry.state().gameConnected());
                assertTrue(registry.state().gameConnected());
                assertEquals("runtime-1", registry.activeSession().orElseThrow().runtimeId());
            }
        }
    }

    @Test
    void hostDisconnectClearsActiveRuntimeSession() throws Exception {
        var registry = new RuntimeRegistry();
        try (var host = RuntimeHost.start(registry, "127.0.0.1", freePort())) {
            try (var socket = new Socket("127.0.0.1", host.port());
                 var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write("{\"type\":\"runtime.hello\",\"runtimeId\":\"runtime-1\",\"runtimeSide\":\"client\",\"supportedScopes\":[\"common\",\"client\"],\"supportedSides\":[\"client\"],\"toolDescriptors\":[],\"state\":{}}\n");
                writer.flush();
                waitUntil(() -> registry.state().gameConnected());
            }
            waitUntil(() -> !registry.state().gameConnected());
        }

        assertFalse(registry.state().gameConnected());
    }

    private int freePort() throws Exception {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void waitUntil(Check check) throws Exception {
        var deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            if (check.ok()) {
                return;
            }
            Thread.sleep(25L);
        }
        throw new AssertionError("Timed out waiting for condition");
    }

    @FunctionalInterface
    private interface Check {
        boolean ok();
    }
}

