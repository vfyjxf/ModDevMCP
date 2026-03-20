package dev.vfyjxf.moddev.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPackagingContractTest {

    @Test
    void bundledRuntimeLibrariesAreNotPublishedAsExternalRuntimeDependencies() throws Exception {
        var rootDir = RepoRootLocator.repoRoot();
        var buildGradle = Files.readString(rootDir.resolve("Mod/build.gradle"));

        assertTrue(buildGradle.contains("compileOnly(\"net.lenni0451:Reflect:"));
        assertTrue(buildGradle.contains("additionalRuntimeClasspath(\"net.lenni0451:Reflect:"));
        assertTrue(buildGradle.contains("jarJar(\"net.lenni0451:Reflect:"));
        assertFalse(buildGradle.contains("implementation(\"net.lenni0451:Reflect:"));
        assertFalse(buildGradle.contains("compileOnly(\"com.google.code.gson:gson:"));
        assertFalse(buildGradle.contains("additionalRuntimeClasspath(\"com.google.code.gson:gson:"));
        assertFalse(buildGradle.contains("jarJar(\"com.google.code.gson:gson:"));
        assertFalse(buildGradle.contains("implementation(\"com.google.code.gson:gson:"));
    }
}

