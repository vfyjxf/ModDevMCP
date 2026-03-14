package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostArchitectureDocsTest {

    @Test
    void readmeDocumentsHostFirstPrimaryWorkflow() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var readme = Files.readString(rootDir.resolve("README.md"));

        assertTrue(readme.contains("host-first architecture"));
        assertTrue(readme.contains(":Server:runStdioMcp"));
        assertTrue(readme.contains("runClient --no-daemon"));
        assertTrue(readme.contains("moddev.status"));
        assertTrue(readme.contains("hostReady"));
        assertFalse(readme.toLowerCase().contains("relay"));
        assertFalse(readme.contains("createGameMcpBridgeLaunchScript"));
        assertFalse(readme.contains("run-game-mcp-bridge.bat"));
        assertFalse(readme.contains("Legacy standalone embedded stdio host"));
    }

    @Test
    void guidesDocumentHostStartupAndPreflight() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var guidesDir = rootDir.resolve("docs").resolve("guides");
        var installGuide = Files.readString(guidesDir.resolve("2026-03-11-simple-agent-install-guide.md"));
        var preflightGuide = Files.readString(guidesDir.resolve("2026-03-11-agent-preflight-checklist.md"));
        var testModGuide = Files.readString(guidesDir.resolve("2026-03-11-testmod-runclient-guide.md"));

        assertTrue(installGuide.contains(":Server:runStdioMcp"));
        assertTrue(installGuide.contains("host"));
        assertTrue(installGuide.contains("moddev.status"));
        assertFalse(installGuide.contains("run-game-mcp-bridge.bat"));

        assertTrue(preflightGuide.contains("moddev.status"));
        assertTrue(preflightGuide.contains("moddev.ui_get_live_screen"));
        assertTrue(preflightGuide.contains("host"));
        assertFalse(preflightGuide.contains("run-game-mcp-bridge.bat"));

        assertTrue(testModGuide.contains("cd TestMod"));
        assertTrue(testModGuide.contains("runClient --no-daemon"));
        assertTrue(testModGuide.toLowerCase().contains("host"));
        assertFalse(testModGuide.toLowerCase().contains("relay"));
        assertFalse(testModGuide.contains("run-game-mcp-bridge.bat"));
    }

    @Test
    void modBuildNoLongerPublishesLegacyBridgeTasks() throws Exception {
        var buildGradle = Files.readString(Path.of("").toAbsolutePath().normalize().resolve("build.gradle"));

        assertFalse(buildGradle.contains("EmbeddedMcpLaunchFiles"));
        assertFalse(buildGradle.contains("embeddedMcpRuntime"));
        assertFalse(buildGradle.contains("runEmbeddedMcpStdio"));
        assertFalse(buildGradle.contains("runGameMcpBridge"));
        assertFalse(buildGradle.contains("createGameMcpBridgeLaunchScript"));
    }
}

