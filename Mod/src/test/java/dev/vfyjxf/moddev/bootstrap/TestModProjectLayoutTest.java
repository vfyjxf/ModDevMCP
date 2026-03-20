package dev.vfyjxf.moddev.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestModProjectLayoutTest {

    @Test
    void testModCompositeProjectExistsWithStandaloneSettings() throws Exception {
        var repoDir = RepoRootLocator.repoRoot().resolve("TestMod");
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
        assertTrue(Files.exists(gradlew) || Files.exists(gradlewBat), "expected gradlew or gradlew.bat in " + repoDir);
        assertTrue(Files.exists(gradlewBat), gradlewBat.toString());
        assertTrue(Files.exists(entrypoint), entrypoint.toString());
        assertTrue(Files.exists(modsToml), modsToml.toString());
        assertTrue(Files.exists(packMcmeta), packMcmeta.toString());
        assertTrue(Files.readString(settingsGradle).contains("rootProject.name = 'TestMod'"));
        assertTrue(Files.readString(settingsGradle).contains("mavenLocal()"));
        assertFalse(Files.readString(buildGradle).contains("dev.vfyjxf.moddevmcp"));
        assertFalse(Files.readString(buildGradle).contains("createMcpClientFiles"));
        assertFalse(Files.readString(gradleProperties).contains("plugin_version="));
    }
}

