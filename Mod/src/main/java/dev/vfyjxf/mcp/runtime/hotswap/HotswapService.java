package dev.vfyjxf.mcp.runtime.hotswap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HotswapService {

    private static final String HOTSWAP_AGENT_CLASS_NAME = "dev.vfyjxf.mcp.agent.HotswapAgent";

    private final HotswapRuntimeConfig config;
    private final ClassLoader agentClassLoader;
    private final String agentClassName;
    private Map<String, Long> baseline;

    public HotswapService(HotswapRuntimeConfig config) {
        this(config, ClassLoader.getSystemClassLoader(), HOTSWAP_AGENT_CLASS_NAME);
    }

    HotswapService(HotswapRuntimeConfig config, ClassLoader agentClassLoader, String agentClassName) {
        this.config = config;
        this.agentClassLoader = agentClassLoader;
        this.agentClassName = agentClassName;
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
        Instrumentation inst = resolveAgentInstrumentation(agentClassLoader, agentClassName);
        Map<String, Object> diagnostics = collectAgentDiagnostics(inst, agentClassLoader, agentClassName);
        if (inst == null) {
            return new ReloadResult(
                    List.of(), List.of(),
                    Map.of("agent", "HotswapAgent not loaded. Ensure -javaagent is configured."),
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
        return config.projectRoot()
                .resolve(isWindows() ? "gradlew.bat" : "gradlew")
                .toAbsolutePath()
                .toString();
    }

    private static String readStream(java.io.InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static Map<String, Object> collectAgentDiagnostics(Instrumentation instrumentation,
                                                               ClassLoader agentClassLoader,
                                                               String agentClassName) {
        Map<String, Object> diagnostics = new HashMap<>();
        diagnostics.put("modAgentClassLoader", "unavailable");
        diagnostics.put("modInstrumentationPresent", false);

        try {
            Class<?> systemAgentClass = Class.forName(agentClassName, false, agentClassLoader);
            diagnostics.put("systemAgentClassLoader", describeClassLoader(systemAgentClass.getClassLoader()));
            diagnostics.put("sameAgentClass", false);
            diagnostics.put("systemInstrumentationPresent", instrumentation != null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            diagnostics.put("systemAgentClassLoader", "unavailable");
            diagnostics.put("sameAgentClass", false);
            diagnostics.put("systemInstrumentationPresent", false);
            diagnostics.put("systemAgentLookupError", exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }

        return Map.copyOf(diagnostics);
    }

    private static Instrumentation resolveAgentInstrumentation(ClassLoader agentClassLoader, String agentClassName) {
        try {
            Class<?> agentClass = Class.forName(agentClassName, false, agentClassLoader);
            return readInstrumentation(agentClass);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
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

    private static Instrumentation readInstrumentation(Class<?> agentClass) throws ReflectiveOperationException {
        Method instrumentationMethod = agentClass.getMethod("instrumentation");
        return (Instrumentation) instrumentationMethod.invoke(null);
    }

    private static String describeClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            return "bootstrap";
        }
        return classLoader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(classLoader));
    }
}
