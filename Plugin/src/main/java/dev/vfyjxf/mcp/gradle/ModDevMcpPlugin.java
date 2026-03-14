package dev.vfyjxf.mcp.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class ModDevMcpPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        ModDevMcpExtension extension =
                project.getExtensions().create("modDevMcp", ModDevMcpExtension.class);
        extension.getProjectRoot().convention(project.getRootProject().getProjectDir());
        extension.getAgentCoordinates().convention(project.provider(() ->
                "dev.vfyjxf:moddevmcp-agent:" + resolveDefaultVersion(project)));
        extension.getJavaCommand().convention(project.provider(ModDevMcpPlugin::resolveDefaultJavaCommand));
        extension.getMcpClientFilesOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("moddevmcp/mcp-clients"));

        project.getTasks().register("createMcpClientFiles", CreateMcpClientFilesTask.class, task -> {
            task.setGroup("mod development");
            task.setDescription("Generates generic MCP client launch files and configuration snippets.");
            task.getServerId().set(extension.getMcpServerId());
            task.getMainClass().set(extension.getMcpMainClass());
            task.getBackendMainClass().set(extension.getBackendMainClass());
            task.getJavaCommand().set(extension.getJavaCommand());
            task.getMcpHost().set(extension.getMcpHost());
            task.getMcpPort().set(extension.getMcpPort());
            task.getMcpProxyPort().set(extension.getMcpProxyPort());
            task.getRuntimeClasspath().from(extension.getMcpRuntimeClasspath());
            task.getOutputDir().set(extension.getMcpClientFilesOutputDir());
        });

        project.getPluginManager().withPlugin("net.neoforged.moddev",
                ignored -> project.afterEvaluate(
                        evaluated -> new dev.vfyjxf.mcp.gradle.neoforge.NeoForgeRunInjector().inject(evaluated, extension)
                ));
    }

    private static String resolveDefaultVersion(Project project) {
        var explicitVersion = firstNonBlank(project.findProperty("moddevmcp.version"));
        if (explicitVersion != null) {
            return explicitVersion;
        }
        var repoVersion = firstNonBlank(project.findProperty("mod_version"));
        if (repoVersion != null) {
            return repoVersion;
        }
        var projectVersion = String.valueOf(project.getVersion());
        if (!projectVersion.isBlank() && !"unspecified".equals(projectVersion)) {
            return projectVersion;
        }
        throw new IllegalStateException("Unable to infer ModDevMCP agent version. Set modDevMcp.agentCoordinates or the moddevmcp.version property.");
    }

    private static String firstNonBlank(Object value) {
        if (value == null) {
            return null;
        }
        var stringValue = String.valueOf(value).trim();
        return stringValue.isEmpty() ? null : stringValue;
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
