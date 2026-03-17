package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostArchitectureDocsTest {

    @Test
    void readmeDocumentsModOwnedServiceArchitecture() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var readme = Files.readString(rootDir.resolve("README.md"));

        assertTrue(readme.toLowerCase().contains("local http service"));
        assertTrue(readme.contains("Mod"));
        assertTrue(readme.contains("/api/v1/status"));
        assertFalse(readme.contains("host-first architecture"));
        assertFalse(readme.contains(":Server:runStdioMcp"));
        assertFalse(readme.contains("moddev.status"));
    }

    @Test
    void rootBuildDocsDropServerAndPluginAsEndUserProducts() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var settingsGradle = Files.readString(rootDir.resolve("settings.gradle"));
        var readme = Files.readString(rootDir.resolve("README.md"));

        assertTrue(settingsGradle.contains("include(\":Mod\""));
        assertTrue(settingsGradle.contains("Server and Plugin stay in-repo only for migration"));
        assertTrue(settingsGradle.contains(":Server"));
        assertTrue(settingsGradle.contains(":Plugin"));

        assertFalse(readme.contains("Server artifact"));
        assertFalse(readme.contains("Gradle plugin id"));
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

