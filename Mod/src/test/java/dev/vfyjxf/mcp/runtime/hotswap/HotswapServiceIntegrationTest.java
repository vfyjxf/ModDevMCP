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

        HotswapService service = new HotswapService(new HotswapRuntimeConfig(tempDir, "compileJava", tempDir));

        var result = service.compile();

        assertEquals(0, result.exitCode(), () -> "Expected wrapper to be executable from project root but stderr was: " + result.stderr());
        assertTrue(result.stdout().contains("wrapper-ok"), () -> "Expected wrapper output but got: " + result.stdout());
    }

    @Test
    void reloadReportsAgentClassloaderDiagnostics() {
        HotswapService service = new HotswapService(new HotswapRuntimeConfig(Path.of("."), ":noop", Path.of(".")));

        var result = service.reload();

        assertTrue(result.diagnostics().containsKey("modAgentClassLoader"));
        assertTrue(result.diagnostics().containsKey("systemAgentClassLoader"));
        assertTrue(result.diagnostics().containsKey("sameAgentClass"));
        assertTrue(result.diagnostics().containsKey("modInstrumentationPresent"));
        assertTrue(result.diagnostics().containsKey("systemInstrumentationPresent"));
    }

    @Test
    void reloadReturnsAgentErrorWhenAgentClassIsUnavailable() {
        HotswapService service = new HotswapService(
                new HotswapRuntimeConfig(Path.of("."), ":noop", Path.of(".")),
                new ClassLoader(null) { },
                "missing.Agent"
        );

        var result = service.reload();

        assertEquals("HotswapAgent not loaded. Ensure -javaagent is configured.", result.errors().get("agent"));
        assertTrue(result.diagnostics().containsKey("systemAgentLookupError"));
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

            HotswapService service = new HotswapService(new HotswapRuntimeConfig(tempDir, ":noop", classesDir));
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
}
