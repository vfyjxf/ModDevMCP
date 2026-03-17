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

        assertTrue(readme.contains("approved migration direction"));
        assertTrue(readme.toLowerCase().contains("local http service"));
        assertTrue(readme.contains("End-user runtime product: `:Mod`"));
        assertTrue(readme.contains("Internal migration modules still in repo: `:Server`, `:Plugin`"));
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
        assertTrue(settingsGradle.contains(":Server"));
        assertTrue(settingsGradle.contains(":Plugin"));
        assertTrue(settingsGradle.toLowerCase().contains("product boundary"));
        assertTrue(settingsGradle.toLowerCase().contains("end-user runtime product"));
        assertTrue(settingsGradle.toLowerCase().contains("migration"));

        assertTrue(readme.contains("End-user runtime product: `:Mod`"));
        assertTrue(readme.contains("Internal migration modules still in repo: `:Server`, `:Plugin`"));
        assertFalse(readme.contains("Server artifact"));
        assertFalse(readme.contains("Gradle plugin id"));
    }
}

