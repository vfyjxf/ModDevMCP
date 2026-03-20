package dev.vfyjxf.moddev.runtime.hotswap;

import java.nio.file.Path;

public record HotswapRuntimeConfig(Path projectRoot, Path gradleRoot, String compileTask, Path classOutputDir) {

    private static final String PROJECT_ROOT_PROPERTY = "moddevmcp.project.root";
    private static final String GRADLE_ROOT_PROPERTY = "moddevmcp.gradle.root";
    private static final String COMPILE_TASK_PROPERTY = "moddevmcp.compile.task";
    private static final String CLASS_OUTPUT_PROPERTY = "moddevmcp.class.output";

    public static HotswapRuntimeConfig fromSystemProperties() {
        Path projectRoot = Path.of(System.getProperty(PROJECT_ROOT_PROPERTY, System.getProperty("user.dir")))
                .toAbsolutePath()
                .normalize();
        String configuredGradleRoot = System.getProperty(GRADLE_ROOT_PROPERTY);
        Path gradleRoot = configuredGradleRoot == null || configuredGradleRoot.isBlank()
                ? projectRoot
                : resolvePath(projectRoot, configuredGradleRoot);
        String compileTask = System.getProperty(COMPILE_TASK_PROPERTY, ":compileJava");

        String classOutput = System.getProperty(CLASS_OUTPUT_PROPERTY);
        Path classOutputDir = classOutput == null || classOutput.isBlank()
                ? projectRoot.resolve("build/classes/java/main")
                : resolvePath(projectRoot, classOutput);

        return new HotswapRuntimeConfig(
                projectRoot,
                gradleRoot.toAbsolutePath().normalize(),
                compileTask,
                classOutputDir.toAbsolutePath().normalize()
        );
    }

    public HotswapRuntimeConfig {
        projectRoot = projectRoot.toAbsolutePath().normalize();
        gradleRoot = gradleRoot.toAbsolutePath().normalize();
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

