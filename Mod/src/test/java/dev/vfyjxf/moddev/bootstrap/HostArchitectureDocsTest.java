package dev.vfyjxf.moddev.bootstrap;

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

        assertTrue(readme.contains("skill-first service model"));
        assertTrue(readme.toLowerCase().contains("local http service"));
        assertTrue(readme.contains("End-user runtime product: `:Mod`"));
        assertTrue(readme.contains("Legacy `Server` and `Plugin` modules are removed from the active build."));
        assertTrue(readme.contains("/api/v1/status"));
        assertTrue(readme.contains("build/moddevmcp/game-instances.json"));
        assertTrue(readme.toLowerCase().contains("client and server use separate ports"));
        assertTrue(readme.contains("default probe"));
        assertTrue(readme.contains("project-local fallback"));
        assertFalse(readme.contains("host-first architecture"));
        assertFalse(readme.contains(":Server:runStdioMcp"));
        assertFalse(readme.contains(":Server"));
        assertFalse(readme.contains(":Plugin"));
        assertFalse(readme.contains("moddev.status"));
    }

    @Test
    void rootBuildIncludesOnlyModProduct() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var settingsGradle = Files.readString(rootDir.resolve("settings.gradle"));
        var readme = Files.readString(rootDir.resolve("README.md"));

        assertTrue(settingsGradle.contains("include(\":Mod\""));
        assertFalse(settingsGradle.contains(":Server"));
        assertFalse(settingsGradle.contains(":Plugin"));
        assertTrue(settingsGradle.toLowerCase().contains("product boundary"));
        assertTrue(settingsGradle.toLowerCase().contains("end-user runtime product"));
        assertFalse(settingsGradle.toLowerCase().contains("migration"));

        assertTrue(readme.contains("End-user runtime product: `:Mod`"));
        assertTrue(readme.contains("Legacy `Server` and `Plugin` modules are removed from the active build."));
        assertFalse(readme.contains("Server artifact"));
        assertFalse(readme.contains("Gradle plugin id"));
    }
}


