package dev.vfyjxf.mcp.gradle.neoforge;

import dev.vfyjxf.mcp.gradle.ModDevMcpExtension;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.Project;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class NeoForgeRunInjector {

    public void inject(Project project, ModDevMcpExtension extension) {
        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        Set<String> selectedRuns = new HashSet<>(extension.getRuns().get());
        neoForge.getRuns().configureEach(run -> configureRun(project, extension, selectedRuns, run));
    }

    private static void configureRun(Project project, ModDevMcpExtension extension,
                                     Set<String> selectedRuns, RunModel run) {
        if (!extension.getEnabled().get() || !selectedRuns.contains(run.getName())) {
            return;
        }

        File projectRoot = extension.getProjectRoot().get();
        File classOutput = resolvePath(projectRoot, extension.getClassOutputDir().get());
        String agentPath = resolveAgentPath(project, extension);

        run.jvmArgument("-javaagent:" + agentPath);
        run.systemProperty("moddevmcp.project.root", projectRoot.getAbsolutePath());
        run.systemProperty("moddevmcp.compile.task", extension.getCompileTask().get());
        run.systemProperty("moddevmcp.class.output", classOutput.getAbsolutePath());
        run.systemProperty("moddevmcp.host", extension.getMcpHost().get());
        run.systemProperty("moddevmcp.port", String.valueOf(extension.getMcpPort().get()));
        project.getTasks().matching(task -> task.getName().equals(runTaskName(run.getName())))
                .configureEach(task -> task.dependsOn("createMcpClientFiles"));
    }

    private static String resolveAgentPath(Project project, ModDevMcpExtension extension) {
        if (extension.getAgentJarPath().isPresent()) {
            return extension.getAgentJarPath().get();
        }

        var coordinates = extension.getAgentCoordinates().get();
        var configuration = project.getConfigurations().detachedConfiguration(
                project.getDependencies().create(coordinates)
        );
        configuration.setTransitive(false);
        var files = configuration.resolve();
        if (files.isEmpty()) {
            throw new IllegalStateException("No ModDevMCP agent artifact resolved from " + coordinates);
        }
        return files.iterator().next().getAbsolutePath();
    }

    private static File resolvePath(File projectRoot, String configuredPath) {
        File path = new File(configuredPath);
        if (path.isAbsolute()) {
            return path;
        }
        return new File(projectRoot, configuredPath);
    }

    private static String runTaskName(String runName) {
        if (runName == null || runName.isBlank()) {
            return "run";
        }
        return "run" + Character.toUpperCase(runName.charAt(0)) + runName.substring(1);
    }
}
