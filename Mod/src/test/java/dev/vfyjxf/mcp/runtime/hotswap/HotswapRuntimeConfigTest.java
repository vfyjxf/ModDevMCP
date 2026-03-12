package dev.vfyjxf.mcp.runtime.hotswap;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HotswapRuntimeConfigTest {

    @Test
    void defaultsToLegacyProjectLayoutWhenNoOverridesArePresent() {
        String originalUserDir = System.getProperty("user.dir");
        String originalProjectRoot = System.getProperty("moddevmcp.project.root");
        String originalCompileTask = System.getProperty("moddevmcp.compile.task");
        String originalClassOutput = System.getProperty("moddevmcp.class.output");
        try {
            System.setProperty("user.dir", "C:/workspace/sample");
            System.clearProperty("moddevmcp.project.root");
            System.clearProperty("moddevmcp.compile.task");
            System.clearProperty("moddevmcp.class.output");

            HotswapRuntimeConfig config = HotswapRuntimeConfig.fromSystemProperties();

            assertEquals(Path.of("C:/workspace/sample").toAbsolutePath().normalize(), config.projectRoot());
            assertEquals(":Mod:compileJava", config.compileTask());
            assertEquals(Path.of("C:/workspace/sample/Mod/build/classes/java/main").toAbsolutePath().normalize(),
                    config.classOutputDir());
        } finally {
            restoreProperty("user.dir", originalUserDir);
            restoreProperty("moddevmcp.project.root", originalProjectRoot);
            restoreProperty("moddevmcp.compile.task", originalCompileTask);
            restoreProperty("moddevmcp.class.output", originalClassOutput);
        }
    }

    @Test
    void usesExplicitSystemPropertyOverrides() {
        String originalUserDir = System.getProperty("user.dir");
        String originalProjectRoot = System.getProperty("moddevmcp.project.root");
        String originalCompileTask = System.getProperty("moddevmcp.compile.task");
        String originalClassOutput = System.getProperty("moddevmcp.class.output");
        try {
            System.setProperty("user.dir", "C:/workspace/ignored");
            System.setProperty("moddevmcp.project.root", "D:/consumer/project");
            System.setProperty("moddevmcp.compile.task", ":Game:compileJava");
            System.setProperty("moddevmcp.class.output", "build/custom/classes");

            HotswapRuntimeConfig config = HotswapRuntimeConfig.fromSystemProperties();

            assertEquals(Path.of("D:/consumer/project").toAbsolutePath().normalize(), config.projectRoot());
            assertEquals(":Game:compileJava", config.compileTask());
            assertEquals(Path.of("D:/consumer/project/build/custom/classes").toAbsolutePath().normalize(),
                    config.classOutputDir());
        } finally {
            restoreProperty("user.dir", originalUserDir);
            restoreProperty("moddevmcp.project.root", originalProjectRoot);
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
