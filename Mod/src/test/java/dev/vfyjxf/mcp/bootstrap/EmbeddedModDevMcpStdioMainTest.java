package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedModDevMcpStdioMainTest {

    @Test
    void embeddedHostExposesBuiltinModTools() throws Exception {
        var input = new PipedInputStream();
        var writer = new PipedOutputStream(input);
        var output = new ByteArrayOutputStream();
        var host = EmbeddedModDevMcpStdioMain.createHost(
                input,
                output
        );

        assertNotNull(host);
        host.serve();
        writer.write("""
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"codex","version":"0.0.0"}}}
                """.getBytes(StandardCharsets.UTF_8));
        writer.flush();
        waitForOutput(output, "\"id\":1", Duration.ofSeconds(10));
        writer.write("""
                {"jsonrpc":"2.0","method":"notifications/initialized"}
                """.getBytes(StandardCharsets.UTF_8));
        writer.flush();
        requestToolsUntilResponse(writer, output, Duration.ofSeconds(10));
        writer.close();

        var raw = output.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("\"moddev.ui_snapshot\""), raw);
        assertTrue(raw.contains("\"moddev.ui_capture\""), raw);
        assertTrue(raw.contains("\"moddev.inventory_action\""), raw);
    }

    @Test
    void readmeDocumentsGenericMcpClientConfigForEmbeddedBootstrap() throws Exception {
        var moduleDir = Path.of("").toAbsolutePath().normalize();
        var readmePath = moduleDir.resolveSibling("README.md");
        var readme = Files.readString(readmePath);

        assertTrue(readme.contains("The primary runtime path is now `game MCP`."));
        assertTrue(readme.contains(":Mod:createGameMcpBridgeLaunchScript"));
        assertTrue(readme.contains("run-game-mcp-bridge.bat"));
        assertTrue(readme.contains("GameMcpBridgeMain"));
        assertTrue(readme.contains("start Minecraft first"));
        assertTrue(readme.contains("moddev.ui_get_live_screen"));
        assertTrue(readme.contains("Legacy standalone embedded stdio host for repository debugging:"));
        assertTrue(readme.contains(":Mod:createEmbeddedMcpLaunchScript"));
        assertTrue(readme.contains("run-embedded-mcp-stdio.bat"));
        assertTrue(readme.contains("This path is for low-level debugging, not the primary real-game workflow."));
        assertTrue(readme.contains("runClient --no-daemon"));
        assertTrue(readme.contains("This is the primary real-game validation path."));
        assertTrue(!readme.contains("StableModDevMcpServerMain"));
        assertTrue(!readme.contains("ClientRuntimeBootstrap"));
        assertTrue(!readme.contains("GameBackendAgentMain"));
    }

    @Test
    void testModGuideDocumentsPrimaryRunClientWorkflow() throws Exception {
        var moduleDir = Path.of("").toAbsolutePath().normalize();
        var guidePath = moduleDir.resolveSibling("docs")
                .resolve("guides")
                .resolve("2026-03-11-testmod-runclient-guide.md");
        var guide = Files.readString(guidePath);

        assertTrue(guide.contains("TestMod RunClient Guide"));
        assertTrue(guide.contains("cd TestMod"));
        assertTrue(guide.contains(".\\gradlew.bat runClient --no-daemon"));
        assertTrue(guide.contains("game MCP endpoint"));
        assertTrue(guide.contains("run-game-mcp-bridge.bat"));
        assertTrue(!guide.contains("stable server"));
    }

    @Test
    void modBuildDocumentsGameMcpBridgeInsteadOfStableBackendLaunchers() throws Exception {
        var moduleDir = Path.of("").toAbsolutePath().normalize();
        var buildGradlePath = moduleDir.resolve("build.gradle");
        var buildGradle = Files.readString(buildGradlePath);

        assertTrue(buildGradle.contains("runGameMcpBridge"));
        assertTrue(buildGradle.contains("writeGameMcpBridgeClasspath"));
        assertTrue(buildGradle.contains("createGameMcpBridgeLaunchScript"));
        assertTrue(buildGradle.contains("GameMcpBridgeMain"));
        assertTrue(!buildGradle.contains("runGameBackendAgent"));
        assertTrue(!buildGradle.contains("createGameBackendAgentLaunchScript"));
        assertTrue(!buildGradle.contains("stableServerConfig"));
    }

    @Test
    void gameMcpGuideDocumentsCurrentPrimaryWorkflow() throws Exception {
        var moduleDir = Path.of("").toAbsolutePath().normalize();
        var guidePath = moduleDir.resolveSibling("docs")
                .resolve("guides")
                .resolve("2026-03-11-game-mcp-guide.md");
        var guide = Files.readString(guidePath);

        assertTrue(guide.contains("Minecraft process hosts MCP directly"));
        assertTrue(guide.contains("run-game-mcp-bridge.bat"));
        assertTrue(guide.contains("moddev.ui_get_live_screen"));
    }

    @Test
    void gameMcpGuideDoesNotMentionStableServerAsPrimaryPath() throws Exception {
        var moduleDir = Path.of("").toAbsolutePath().normalize();
        var guidePath = moduleDir.resolveSibling("docs")
                .resolve("guides")
                .resolve("2026-03-11-game-mcp-guide.md");
        var guide = Files.readString(guidePath);

        assertTrue(!guide.contains("StableModDevMcpServerMain"));
        assertTrue(!guide.contains("GameBackendAgentMain"));
        assertTrue(!guide.contains("installLocalStableServer"));
    }

    private void waitForOutput(ByteArrayOutputStream output, String expectedFragment, Duration timeout) {
        var deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (output.toString(StandardCharsets.UTF_8).contains(expectedFragment)) {
                return;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for stdio output", exception);
            }
        }
        throw new AssertionError("Timed out waiting for stdio output: " + output.toString(StandardCharsets.UTF_8));
    }

    private void requestToolsUntilResponse(PipedOutputStream writer, ByteArrayOutputStream output, Duration timeout) throws Exception {
        var deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            writer.write("""
                    {"jsonrpc":"2.0","id":2,"method":"tools/list"}
                    """.getBytes(StandardCharsets.UTF_8));
            writer.flush();
            var waitUntil = Instant.now().plusMillis(250L);
            while (Instant.now().isBefore(waitUntil)) {
                if (output.toString(StandardCharsets.UTF_8).contains("\"moddev.ui_snapshot\"")) {
                    return;
                }
                Thread.sleep(25L);
            }
        }
        throw new AssertionError("Timed out waiting for tools/list response: " + output.toString(StandardCharsets.UTF_8));
    }
}
