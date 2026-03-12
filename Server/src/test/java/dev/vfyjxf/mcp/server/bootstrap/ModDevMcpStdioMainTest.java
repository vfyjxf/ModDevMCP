package dev.vfyjxf.mcp.server.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDevMcpStdioMainTest {

    @Test
    void bootstrapCreatesStdioHostThatCanAnswerInitialize() {
        var output = new ByteArrayOutputStream();
        var host = ModDevMcpStdioMain.createHost(
                new ByteArrayInputStream("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"codex","version":"0.0.0"}}}
                        """.getBytes(StandardCharsets.UTF_8)),
                output
        );

        assertNotNull(host);
        host.serve();
        waitForOutput(output, Duration.ofSeconds(2));

        var raw = output.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("\"serverInfo\""));
        assertTrue(raw.contains("\"id\":1"));
    }

    @Test
    void readmeDocumentsGameHostedPrimaryEntryPoint() throws Exception {
        var moduleDir = Path.of("").toAbsolutePath().normalize();
        var readmePath = moduleDir.resolveSibling("README.md");
        var readme = Files.readString(readmePath);

        assertTrue(readme.contains("[mcp_servers.moddevmcp]"));
        assertTrue(readme.contains("ModDevMcpStdioMain"));
        assertTrue(readme.contains(":Mod:createGameMcpBridgeLaunchScript"));
        assertTrue(readme.contains("run-game-mcp-bridge.bat"));
        assertTrue(readme.contains("GameMcpBridgeMain"));
        assertTrue(readme.contains("game MCP"));
        assertTrue(!readme.contains("StableModDevMcpServerMain"));
        assertTrue(!readme.contains("StableMcpStdioBridgeMain"));
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
