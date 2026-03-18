package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPackagingContractTest {

    @Test
    void bundledRuntimeLibrariesAreNotPublishedAsExternalRuntimeDependencies() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var buildGradle = Files.readString(rootDir.resolve("Mod/build.gradle"));

        assertTrue(buildGradle.contains("compileOnly(\"net.lenni0451:Reflect:"));
        assertTrue(buildGradle.contains("additionalRuntimeClasspath(\"net.lenni0451:Reflect:"));
        assertTrue(buildGradle.contains("jarJar(\"net.lenni0451:Reflect:"));
        assertFalse(buildGradle.contains("implementation(\"net.lenni0451:Reflect:"));
        assertTrue(buildGradle.contains("compileOnly(\"com.google.code.gson:gson:"));
        assertFalse(buildGradle.contains("additionalRuntimeClasspath(\"com.google.code.gson:gson:"));
        assertFalse(buildGradle.contains("jarJar(\"com.google.code.gson:gson:"));
    }
}
