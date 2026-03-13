package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestModProjectLayoutTest {

    @Test
    void testModCompositeProjectExistsWithStandaloneSettings() throws Exception {
        var repoDir = Path.of("").toAbsolutePath().normalize().resolveSibling("TestMod");
        var settingsGradle = repoDir.resolve("settings.gradle");
        var buildGradle = repoDir.resolve("build.gradle");
        var gradleProperties = repoDir.resolve("gradle.properties");
        var gradlew = repoDir.resolve("gradlew");
        var gradlewBat = repoDir.resolve("gradlew.bat");
        var entrypoint = repoDir.resolve("src/main/java/dev/vfyjxf/testmod/TestModEntrypoint.java");
        var modsToml = repoDir.resolve("src/main/templates/META-INF/neoforge.mods.toml");
        var packMcmeta = repoDir.resolve("src/main/resources/pack.mcmeta");

        assertTrue(Files.exists(settingsGradle), settingsGradle.toString());
        assertTrue(Files.exists(buildGradle), buildGradle.toString());
        assertTrue(Files.exists(gradleProperties), gradleProperties.toString());
        assertTrue(Files.exists(gradlew), gradlew.toString());
        assertTrue(Files.exists(gradlewBat), gradlewBat.toString());
        assertTrue(Files.exists(entrypoint), entrypoint.toString());
        assertTrue(Files.exists(modsToml), modsToml.toString());
        assertTrue(Files.exists(packMcmeta), packMcmeta.toString());
        assertTrue(Files.readString(settingsGradle).contains("rootProject.name = 'TestMod'"));
        assertTrue(Files.readString(settingsGradle).contains("includeBuild(\"..\")"));
    }
}
