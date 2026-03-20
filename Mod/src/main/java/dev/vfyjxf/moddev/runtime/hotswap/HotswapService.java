package dev.vfyjxf.moddev.runtime.hotswap;

import net.lenni0451.reflect.Agents;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class HotswapService {

    private static final String INSTRUMENTATION_PROVIDER = "Reflect Agents";

    private final HotswapRuntimeConfig config;
    private final Supplier<Instrumentation> instrumentationSupplier;
    private final String instrumentationProvider;
    private Map<String, Long> baseline;

    public HotswapService(HotswapRuntimeConfig config) {
        this(config, () -> {
            try {
                return Agents.getInstrumentation();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }, INSTRUMENTATION_PROVIDER);
    }

    HotswapService(HotswapRuntimeConfig config, Supplier<Instrumentation> instrumentationSupplier,
                   String instrumentationProvider) {
        this.config = config;
        this.instrumentationSupplier = instrumentationSupplier;
        this.instrumentationProvider = instrumentationProvider;
        this.baseline = new HashMap<>();
    }

    public record CompileResult(int exitCode, String stdout, String stderr) {
    }

    public record ReloadResult(List<String> reloadedClasses, List<String> notYetLoaded,
                               Map<String, String> errors, Map<String, Object> capabilities,
                               Map<String, Object> diagnostics) {
    }

    public CompileResult compile() {
        String gradleCommand = resolveGradleCommand();
        ProcessBuilder pb = new ProcessBuilder(gradleCommand, config.compileTask());
        pb.directory(config.gradleRoot().toFile());
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
        var instrumentationResolution = resolveInstrumentation();
        Instrumentation inst = instrumentationResolution.instrumentation();
        Map<String, Object> diagnostics = collectInstrumentationDiagnostics(
                inst,
                instrumentationResolution.provider(),
                instrumentationResolution.error()
        );
        if (inst == null) {
            return new ReloadResult(
                    List.of(), List.of(),
                    Map.of("instrumentation", "Instrumentation is unavailable from Reflect Agents."),
                    Map.of(),
                    diagnostics
            );
        }

        Map<String, Object> capabilities = detectCapabilities(inst);
        Map<String, byte[]> changed = ClassFileScanner.readChanged(config.classOutputDir(), baseline);

        if (changed.isEmpty()) {
            return new ReloadResult(List.of(), List.of(), Map.of(), capabilities, diagnostics);
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

        return new ReloadResult(reloaded, notYetLoaded, errors, capabilities, diagnostics);
    }

    public void snapshotTimestamps() {
        this.baseline = ClassFileScanner.scan(config.classOutputDir());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private String resolveGradleCommand() {
        return config.gradleRoot()
                .resolve(isWindows() ? "gradlew.bat" : "gradlew")
                .toAbsolutePath()
                .toString();
    }

    private static String readStream(java.io.InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private InstrumentationResolution resolveInstrumentation() {
        try {
            return new InstrumentationResolution(instrumentationSupplier.get(), instrumentationProvider, null);
        } catch (RuntimeException exception) {
            return new InstrumentationResolution(null, instrumentationProvider,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static Map<String, Object> collectInstrumentationDiagnostics(Instrumentation instrumentation,
                                                                         String provider,
                                                                         String error) {
        Map<String, Object> diagnostics = new HashMap<>();
        diagnostics.put("instrumentationProvider", provider);
        diagnostics.put("instrumentationPresent", instrumentation != null);
        if (error != null && !error.isBlank()) {
            diagnostics.put("instrumentationError", error);
        } else if (instrumentation == null) {
            diagnostics.put("instrumentationError", "Instrumentation provider returned null.");
        }
        return Map.copyOf(diagnostics);
    }

    private static Map<String, Object> detectCapabilities(Instrumentation instrumentation) {
        Map<String, Object> map = new HashMap<>();
        boolean enhancedHotswap = isEnhancedHotswap();
        map.put("vmName", System.getProperty("java.vm.name", "unknown"));
        map.put("canRedefineClasses", instrumentation.isRedefineClassesSupported());
        map.put("canRetransformClasses", instrumentation.isRetransformClassesSupported());
        map.put("enhancedHotswap", enhancedHotswap);
        map.put("canAddMethods", enhancedHotswap);
        map.put("canAddFields", enhancedHotswap);
        return Map.copyOf(map);
    }

    private static boolean isEnhancedHotswap() {
        String vmName = System.getProperty("java.vm.name", "");
        if (vmName.contains("JBR")) {
            return true;
        }
        String dcevmVersion = System.getProperty("dcevm.version");
        return dcevmVersion != null && !dcevmVersion.isEmpty();
    }

    private record InstrumentationResolution(Instrumentation instrumentation, String provider, String error) {
    }
}

