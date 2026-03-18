package dev.vfyjxf.mcp.server.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ModDevMcpStdioMainTest {

    @Test
    void bootstrapCreatesStdioHostThatCanAnswerInitialize() {
        var output = new ByteArrayOutputStream();
        var host = ModDevMcpStdioMain.createHost(
                new ByteArrayInputStream("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"codex","version":"0.0.0"}}}
                        """.getBytes(StandardCharsets.UTF_8)),
                output,
                new HostEndpointConfig("127.0.0.1", freePort())
        );

        assertNotNull(host);
        host.serve();
        waitForOutput(output, Duration.ofSeconds(2));

        var raw = output.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("\"serverInfo\""));
        assertTrue(raw.contains("\"id\":1"));
    }

    @Test
    void bootstrapStartsRuntimeHostListenerAlongsideStdioHost() throws Exception {
        var hostPort = freePort();
        var host = ModDevMcpStdioMain.createHost(
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(),
                new HostEndpointConfig("127.0.0.1", hostPort)
        );
        try {
            try (var socket = new Socket("127.0.0.1", hostPort)) {
                assertTrue(socket.isConnected());
            }
        } finally {
            if (host instanceof AutoCloseable closeable) {
                closeable.close();
            }
        }
    }

    @Test
    void bootstrapHandlesSequentialJsonLineToolRequests() {
        var output = new ByteArrayOutputStream();
        var host = ModDevMcpStdioMain.createHost(
                new ByteArrayInputStream(("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"codex","version":"0.0.0"}}}
                        {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
                        {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"moddev.status","arguments":{}}}
                        """).getBytes(StandardCharsets.UTF_8)),
                output,
                new HostEndpointConfig("127.0.0.1", freePort())
        );

        host.serve();
        waitForOutput(output, Duration.ofSeconds(3));

        var raw = output.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("\"id\":1"));
        assertTrue(raw.contains("\"id\":2"));
        assertTrue(raw.contains("\"id\":3"));
        assertTrue(raw.contains("\"moddev.status\""));
        assertTrue(raw.contains("\"hostReady\""));
    }

    @Test
    void readmeDocumentsServiceFirstPrimaryEntryPoint() throws Exception {
        var moduleDir = Path.of("").toAbsolutePath().normalize();
        var readmePath = moduleDir.resolveSibling("README.md");
        var readme = Files.readString(readmePath);

        assertTrue(readme.contains("skill-first service model"));
        assertTrue(readme.contains("/api/v1/status"));
        assertTrue(readme.contains("moddev-entry"));
        assertFalse(readme.contains("host-first architecture"));
        assertFalse(readme.contains(":Server:runStdioMcp"));
        assertFalse(readme.contains("ModDevMcpStdioMain"));
        assertFalse(readme.contains("moddev.status"));
        assertFalse(readme.contains("hostReady"));
    }

    private int freePort() {
        try (var serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void waitForOutput(ByteArrayOutputStream output, Duration timeout) {
        var deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (output.size() > 0) {
                return;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for stdio output", exception);
            }
        }
        throw new AssertionError("Timed out waiting for stdio output");
    }
}



