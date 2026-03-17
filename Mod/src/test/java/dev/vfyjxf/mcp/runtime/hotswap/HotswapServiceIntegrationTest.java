package dev.vfyjxf.mcp.runtime.hotswap;

import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotswapServiceIntegrationTest {

    @Test
    void compileUsesWrapperFromConfiguredProjectRootOnWindows() throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }

        Path tempDir = Files.createTempDirectory("hotswap-compile-it-");
        Path wrapper = tempDir.resolve("gradlew.bat");
        Files.writeString(wrapper, """
                @echo off
                echo wrapper-ok
                exit /b 0
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        HotswapService service = new HotswapService(new HotswapRuntimeConfig(tempDir, tempDir, "compileJava", tempDir));

        var result = service.compile();

        assertEquals(0, result.exitCode(), () -> "Expected wrapper to be executable from project root but stderr was: " + result.stderr());
        assertTrue(result.stdout().contains("wrapper-ok"), () -> "Expected wrapper output but got: " + result.stdout());
    }

    @Test
    void compileUsesWrapperFromConfiguredGradleRootOnWindows() throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }

        String originalProjectRoot = System.getProperty("moddevmcp.project.root");
        String originalGradleRoot = System.getProperty("moddevmcp.gradle.root");
        String originalCompileTask = System.getProperty("moddevmcp.compile.task");
        String originalClassOutput = System.getProperty("moddevmcp.class.output");
        try {
            Path gradleRoot = Files.createTempDirectory("hotswap-gradle-root-");
            Path projectRoot = gradleRoot.resolve("subproject");
            Files.createDirectories(projectRoot);
            Path wrapper = gradleRoot.resolve("gradlew.bat");
            Files.writeString(wrapper, """
                    @echo off
                    echo wrapper-ok
                    exit /b 0
                    """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            System.setProperty("moddevmcp.project.root", projectRoot.toString());
            System.setProperty("moddevmcp.gradle.root", gradleRoot.toString());
            System.setProperty("moddevmcp.compile.task", ":subproject:compileJava");
            System.setProperty("moddevmcp.class.output", projectRoot.resolve("build/classes/java/main").toString());

            HotswapService service = new HotswapService(HotswapRuntimeConfig.fromSystemProperties());

            var result = service.compile();

            assertEquals(0, result.exitCode(), () -> "Expected wrapper to be executable from gradle root but stderr was: " + result.stderr());
            assertTrue(result.stdout().contains("wrapper-ok"), () -> "Expected wrapper output but got: " + result.stdout());
        } finally {
            restoreProperty("moddevmcp.project.root", originalProjectRoot);
            restoreProperty("moddevmcp.gradle.root", originalGradleRoot);
            restoreProperty("moddevmcp.compile.task", originalCompileTask);
            restoreProperty("moddevmcp.class.output", originalClassOutput);
        }
    }

    @Test
    void compileRunsFromConfiguredGradleRootWhenGradleRootIsOutsideProjectTreeOnWindows() throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }

        Path tempDir = Files.createTempDirectory("hotswap-external-gradle-root-it-");
        Path gradleRoot = tempDir.resolve("gradle-root");
        Path projectRoot = tempDir.resolve("project-root");
        Files.createDirectories(gradleRoot);
        Files.createDirectories(projectRoot);
        Files.writeString(gradleRoot.resolve("settings.gradle"), "rootProject.name = 'consumer'");
        Files.writeString(gradleRoot.resolve("gradlew.bat"), """
                @echo off
                if exist settings.gradle (
                  echo gradle-root-ok
                  exit /b 0
                )
                echo wrong-working-dir=%cd%
                exit /b 7
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        HotswapService service = new HotswapService(
                new HotswapRuntimeConfig(projectRoot, gradleRoot, ":subproject:compileJava", projectRoot)
        );

        var result = service.compile();

        assertEquals(0, result.exitCode(), () -> "Expected compile to run from gradle root but stderr was: " + result.stderr()
                + " stdout was: " + result.stdout());
        assertTrue(result.stdout().contains("gradle-root-ok"), () -> "Expected gradle root marker but got: " + result.stdout());
    }

    @Test
    void reloadReportsInstrumentationDiagnostics() {
        HotswapService service = new HotswapService(new HotswapRuntimeConfig(Path.of("."), Path.of("."), ":noop", Path.of(".")));

        var result = service.reload();

        assertTrue(result.diagnostics().containsKey("instrumentationPresent"));
        assertTrue(result.diagnostics().containsKey("instrumentationProvider"));
    }

    @Test
    void reloadReturnsInstrumentationErrorWhenUnavailable() {
        HotswapService service = new HotswapService(
                new HotswapRuntimeConfig(Path.of("."), Path.of("."), ":noop", Path.of(".")),
                () -> null,
                "Reflect Agents"
        );

        var result = service.reload();

        assertEquals("Instrumentation is unavailable from Reflect Agents.", result.errors().get("instrumentation"));
        assertTrue(result.diagnostics().containsKey("instrumentationError"));
    }

    @Test
    void reloadReplacesMethodBodyForAlreadyLoadedClass() throws Exception {
        Path tempDir = Files.createTempDirectory("hotswap-it-");
        Path classesDir = tempDir.resolve("classes");
        Path sourceDir = tempDir.resolve("src");
        Path sourceFile = sourceDir.resolve("demo/Calculator.java");

        compileSource(sourceFile, classesDir, """
                package demo;

                public class Calculator {
                    public int op(int a, int b) {
                        return a + b;
                    }
                }
                """);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()})) {
            Class<?> calculatorClass = classLoader.loadClass("demo.Calculator");
            Object calculator = calculatorClass.getDeclaredConstructor().newInstance();

            assertEquals(3, invokeOp(calculatorClass, calculator, 1, 2));

            HotswapService service = new HotswapService(new HotswapRuntimeConfig(tempDir, tempDir, ":noop", classesDir));
            service.snapshotTimestamps();

            Thread.sleep(50L);
            compileSource(sourceFile, classesDir, """
                    package demo;

                    public class Calculator {
                        public int op(int a, int b) {
                            return a - b;
                        }
                    }
                    """);

            var result = service.reload();

            assertTrue(result.errors().isEmpty(), () -> "Expected no reload errors but got: " + result.errors());
            assertTrue(result.reloadedClasses().contains("demo.Calculator"),
                    () -> "Expected demo.Calculator to be reloaded but got: " + result.reloadedClasses());
            assertEquals(-1, invokeOp(calculatorClass, calculator, 1, 2));
        }
    }

    private static int invokeOp(Class<?> calculatorClass, Object instance, int a, int b) throws Exception {
        return (int) calculatorClass.getMethod("op", int.class, int.class).invoke(instance, a, b);
    }

    private static void compileSource(Path sourceFile, Path classesDir, String source) throws IOException {
        Files.createDirectories(sourceFile.getParent());
        Files.createDirectories(classesDir);
        Files.writeString(sourceFile, source,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("System JavaCompiler is not available");
        }

        int exitCode = compiler.run(null, null, null,
                "-d", classesDir.toString(),
                sourceFile.toString());
        if (exitCode != 0) {
            throw new IllegalStateException("Compilation failed with exit code " + exitCode);
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
