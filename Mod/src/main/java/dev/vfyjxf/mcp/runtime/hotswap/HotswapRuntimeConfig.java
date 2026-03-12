package dev.vfyjxf.mcp.runtime.hotswap;

import java.nio.file.Path;

public record HotswapRuntimeConfig(Path projectRoot, String compileTask, Path classOutputDir) {

    private static final String PROJECT_ROOT_PROPERTY = "moddevmcp.project.root";
    private static final String COMPILE_TASK_PROPERTY = "moddevmcp.compile.task";
    private static final String CLASS_OUTPUT_PROPERTY = "moddevmcp.class.output";

    public static HotswapRuntimeConfig fromSystemProperties() {
        Path projectRoot = Path.of(System.getProperty(PROJECT_ROOT_PROPERTY, System.getProperty("user.dir")))
                .toAbsolutePath()
                .normalize();
        String compileTask = System.getProperty(COMPILE_TASK_PROPERTY, ":Mod:compileJava");

        String classOutput = System.getProperty(CLASS_OUTPUT_PROPERTY);
        Path classOutputDir = classOutput == null || classOutput.isBlank()
                ? projectRoot.resolve("Mod/build/classes/java/main")
                : resolvePath(projectRoot, classOutput);

        return new HotswapRuntimeConfig(projectRoot, compileTask, classOutputDir.toAbsolutePath().normalize());
    }

    public HotswapRuntimeConfig {
        projectRoot = projectRoot.toAbsolutePath().normalize();
        classOutputDir = classOutputDir.toAbsolutePath().normalize();
    }

    private static Path resolvePath(Path projectRoot, String configuredPath) {
        Path path = Path.of(configuredPath);
        if (path.isAbsolute()) {
            return path;
        }
        return projectRoot.resolve(path);
    }
}
