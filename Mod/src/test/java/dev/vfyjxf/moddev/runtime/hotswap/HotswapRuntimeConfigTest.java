package dev.vfyjxf.moddev.runtime.hotswap;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HotswapRuntimeConfigTest {

    @Test
    void defaultsToLegacyProjectLayoutWhenNoOverridesArePresent() {
        String originalUserDir = System.getProperty("user.dir");
        String originalProjectRoot = System.getProperty("moddevmcp.project.root");
        String originalGradleRoot = System.getProperty("moddevmcp.gradle.root");
        String originalCompileTask = System.getProperty("moddevmcp.compile.task");
        String originalClassOutput = System.getProperty("moddevmcp.class.output");
        try {
            System.setProperty("user.dir", "C:/workspace/sample");
            System.clearProperty("moddevmcp.project.root");
            System.clearProperty("moddevmcp.gradle.root");
            System.clearProperty("moddevmcp.compile.task");
            System.clearProperty("moddevmcp.class.output");

            HotswapRuntimeConfig config = HotswapRuntimeConfig.fromSystemProperties();

            assertEquals(Path.of("C:/workspace/sample").toAbsolutePath().normalize(), config.projectRoot());
            assertEquals(Path.of("C:/workspace/sample").toAbsolutePath().normalize(), config.gradleRoot());
            assertEquals(":compileJava", config.compileTask());
            assertEquals(Path.of("C:/workspace/sample/build/classes/java/main").toAbsolutePath().normalize(),
                    config.classOutputDir());
        } finally {
            restoreProperty("user.dir", originalUserDir);
            restoreProperty("moddevmcp.project.root", originalProjectRoot);
            restoreProperty("moddevmcp.gradle.root", originalGradleRoot);
            restoreProperty("moddevmcp.compile.task", originalCompileTask);
            restoreProperty("moddevmcp.class.output", originalClassOutput);
        }
    }

    @Test
    void usesExplicitSystemPropertyOverrides() {
        String originalUserDir = System.getProperty("user.dir");
        String originalProjectRoot = System.getProperty("moddevmcp.project.root");
        String originalGradleRoot = System.getProperty("moddevmcp.gradle.root");
        String originalCompileTask = System.getProperty("moddevmcp.compile.task");
        String originalClassOutput = System.getProperty("moddevmcp.class.output");
        try {
            System.setProperty("user.dir", "C:/workspace/ignored");
            System.setProperty("moddevmcp.project.root", "D:/consumer/project");
            System.setProperty("moddevmcp.gradle.root", "D:/consumer");
            System.setProperty("moddevmcp.compile.task", ":Game:compileJava");
            System.setProperty("moddevmcp.class.output", "build/custom/classes");

            HotswapRuntimeConfig config = HotswapRuntimeConfig.fromSystemProperties();

            assertEquals(Path.of("D:/consumer/project").toAbsolutePath().normalize(), config.projectRoot());
            assertEquals(Path.of("D:/consumer").toAbsolutePath().normalize(), config.gradleRoot());
            assertEquals(":Game:compileJava", config.compileTask());
            assertEquals(Path.of("D:/consumer/project/build/custom/classes").toAbsolutePath().normalize(),
                    config.classOutputDir());
        } finally {
            restoreProperty("user.dir", originalUserDir);
            restoreProperty("moddevmcp.project.root", originalProjectRoot);
            restoreProperty("moddevmcp.gradle.root", originalGradleRoot);
            restoreProperty("moddevmcp.compile.task", originalCompileTask);
            restoreProperty("moddevmcp.class.output", originalClassOutput);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}

