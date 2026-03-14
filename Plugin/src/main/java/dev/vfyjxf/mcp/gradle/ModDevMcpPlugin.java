package dev.vfyjxf.mcp.gradle;

import dev.vfyjxf.mcp.gradle.neoforge.NeoForgeRunInjector;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

public final class ModDevMcpPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "modDevMcp";
    private static final String RUNTIME_CONFIGURATION_NAME = "modDevMcpRuntime";
    private static final String CREATE_CLIENT_FILES_TASK_NAME = "createMcpClientFiles";
    private static final String DEFAULT_SERVER_VERSION = "0.1.1";
    private static final String DEFAULT_SERVER_COORDINATES = "dev.vfyjxf:moddevmcp-server:" + DEFAULT_SERVER_VERSION;
    private static final String DEFAULT_SERVER_ID = "moddevmcp";
    private static final String DEFAULT_GATEWAY_MAIN_CLASS = "dev.vfyjxf.mcp.server.bootstrap.ModDevMcpGatewayMain";
    private static final String DEFAULT_BACKEND_MAIN_CLASS = "dev.vfyjxf.mcp.server.bootstrap.ModDevMcpBackendMain";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 47653;
    private static final int DEFAULT_PROXY_PORT = 47654;

    @Override
    public void apply(Project project) {
        var runtimeClasspathConfiguration = project.getConfigurations().maybeCreate(RUNTIME_CONFIGURATION_NAME);
        runtimeClasspathConfiguration.setCanBeConsumed(false);
        runtimeClasspathConfiguration.setCanBeResolved(true);
        runtimeClasspathConfiguration.setVisible(false);
        runtimeClasspathConfiguration.defaultDependencies(dependencies ->
                dependencies.add(project.getDependencies().create(DEFAULT_SERVER_COORDINATES))
        );

        ModDevMcpDefaults defaults = buildDefaults(project, runtimeClasspathConfiguration);
        ModDevMcpExtension extension =
                project.getExtensions().create(EXTENSION_NAME, ModDevMcpExtension.class);

        TaskProvider<CreateMcpClientFilesTask> createClientFilesTask =
                registerCreateClientFilesTask(project, defaults);

        project.getPluginManager().withPlugin("net.neoforged.moddev",
                ignored -> project.afterEvaluate(
                        evaluated -> new NeoForgeRunInjector().inject(evaluated, extension, defaults, createClientFilesTask)
                ));
    }

    private static TaskProvider<CreateMcpClientFilesTask> registerCreateClientFilesTask(Project project,
                                                                                         ModDevMcpDefaults defaults) {
        return project.getTasks().register(CREATE_CLIENT_FILES_TASK_NAME, CreateMcpClientFilesTask.class, task -> {
            task.setGroup("mod development");
            task.setDescription("Generates generic MCP client launch files and configuration snippets.");
            task.getServerId().convention(defaults.serverId());
            task.getMainClass().convention(defaults.gatewayMainClass());
            task.getBackendMainClass().convention(defaults.backendMainClass());
            task.getJavaCommand().convention(defaults.javaCommand());
            task.getMcpHost().convention(defaults.host());
            task.getMcpPort().convention(defaults.port());
            task.getMcpProxyPort().convention(defaults.proxyPort());
            task.getRuntimeClasspath().from(defaults.runtimeClasspath());
            task.getOutputDir().convention(defaults.clientFilesOutputDir());
        });
    }

    private static ModDevMcpDefaults buildDefaults(Project project, FileCollection runtimeConfiguration) {
        var projectDirectory = project.getLayout().getProjectDirectory();
        var compileTaskPath = project.provider(() -> {
            var task = project.getTasks().findByName("compileJava");
            return task != null ? task.getPath() : ":compileJava";
        });
        var classOutputDir = project.provider(() -> {
            var task = project.getTasks().findByName("compileJava");
            if (task instanceof JavaCompile compileTask) {
                return compileTask.getDestinationDirectory().get();
            }
            return project.getLayout().getBuildDirectory().dir("classes/java/main").get();
        });
        var outputDir = project.getLayout().getBuildDirectory().dir("moddevmcp/mcp-clients");
        return new ModDevMcpDefaults(
                projectDirectory,
                compileTaskPath,
                classOutputDir,
                project.provider(() -> DEFAULT_SERVER_ID),
                project.provider(() -> DEFAULT_GATEWAY_MAIN_CLASS),
                project.provider(() -> DEFAULT_BACKEND_MAIN_CLASS),
                project.provider(ModDevMcpPlugin::resolveDefaultJavaCommand),
                project.provider(() -> DEFAULT_HOST),
                project.provider(() -> DEFAULT_PORT),
                project.provider(() -> DEFAULT_PROXY_PORT),
                runtimeConfiguration,
                outputDir
        );
    }

    private static String resolveDefaultJavaCommand() {
        var javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isBlank()) {
            return "java";
        }
        var executableName = isWindows() ? "java.exe" : "java";
        return new java.io.File(new java.io.File(javaHome, "bin"), executableName).getAbsolutePath();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
