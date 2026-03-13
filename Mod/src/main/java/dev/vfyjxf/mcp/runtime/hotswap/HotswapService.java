package dev.vfyjxf.mcp.runtime.hotswap;

import dev.vfyjxf.mcp.agent.HotswapAgent;
import dev.vfyjxf.mcp.agent.HotswapCapabilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HotswapService {

    private final HotswapRuntimeConfig config;
    private Map<String, Long> baseline;

    public HotswapService(HotswapRuntimeConfig config) {
        this.config = config;
        this.baseline = new HashMap<>();
    }

    public record CompileResult(int exitCode, String stdout, String stderr) {
    }

    public record ReloadResult(List<String> reloadedClasses, List<String> notYetLoaded,
                               Map<String, String> errors, Map<String, Object> capabilities) {
    }

    public CompileResult compile() {
        String gradleCommand = isWindows() ? "gradlew.bat" : "./gradlew";
        ProcessBuilder pb = new ProcessBuilder(gradleCommand, config.compileTask());
        pb.directory(config.projectRoot().toFile());
        try {
            Process process = pb.start();
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            int exitCode = process.waitFor();
            return new CompileResult(exitCode, stdout, stderr);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CompileResult(-1, "", "Compilation interrupted");
        }
    }

    public ReloadResult reload() {
        Instrumentation inst = HotswapAgent.instrumentation();
        if (inst == null) {
            return new ReloadResult(
                    List.of(), List.of(),
                    Map.of("agent", "HotswapAgent not loaded. Ensure -javaagent is configured."),
                    Map.of()
            );
        }

        HotswapCapabilities capabilities = HotswapCapabilities.detect(inst);
        Map<String, byte[]> changed = ClassFileScanner.readChanged(config.classOutputDir(), baseline);

        if (changed.isEmpty()) {
            return new ReloadResult(List.of(), List.of(), Map.of(), capabilities.toMap());
        }

        Map<String, Class<?>> loadedClassMap = new HashMap<>();
        for (Class<?> c : inst.getAllLoadedClasses()) {
            loadedClassMap.put(c.getName(), c);
        }

        List<ClassDefinition> definitions = new ArrayList<>();
        List<String> reloaded = new ArrayList<>();
        List<String> notYetLoaded = new ArrayList<>();
        Map<String, String> errors = new HashMap<>();

        for (var entry : changed.entrySet()) {
            String className = ClassFileScanner.classFileToClassName(entry.getKey());
            Class<?> loadedClass = loadedClassMap.get(className);
            if (loadedClass == null) {
                notYetLoaded.add(className);
            } else {
                definitions.add(new ClassDefinition(loadedClass, entry.getValue()));
                reloaded.add(className);
            }
        }

        if (!definitions.isEmpty()) {
            try {
                inst.redefineClasses(definitions.toArray(new ClassDefinition[0]));
            } catch (Exception e) {
                errors.put("redefine", e.getMessage());
                reloaded.clear();
            }
        }

        snapshotTimestamps();

        return new ReloadResult(reloaded, notYetLoaded, errors, capabilities.toMap());
    }

    public void snapshotTimestamps() {
        this.baseline = ClassFileScanner.scan(config.classOutputDir());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String readStream(java.io.InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
