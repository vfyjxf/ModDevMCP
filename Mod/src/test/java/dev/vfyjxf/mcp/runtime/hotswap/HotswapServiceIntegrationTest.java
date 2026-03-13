package dev.vfyjxf.mcp.runtime.hotswap;

import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotswapServiceIntegrationTest {

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
