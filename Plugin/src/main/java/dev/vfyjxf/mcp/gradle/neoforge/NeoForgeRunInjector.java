package dev.vfyjxf.mcp.gradle.neoforge;

import dev.vfyjxf.mcp.gradle.ModDevMcpExtension;
import dev.vfyjxf.mcp.gradle.CreateMcpClientFilesTask;
import dev.vfyjxf.mcp.gradle.ModDevMcpDefaults;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class NeoForgeRunInjector {

    public void inject(Project project, ModDevMcpExtension extension, ModDevMcpDefaults defaults,
                       TaskProvider<CreateMcpClientFilesTask> createMcpClientFilesTask) {
        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        Set<String> selectedRuns = new HashSet<>(extension.getRuns().get());
        neoForge.getRuns().configureEach(run -> configureRun(project, extension, defaults, selectedRuns, run, createMcpClientFilesTask));
    }

    private static void configureRun(Project project, ModDevMcpExtension extension, ModDevMcpDefaults defaults,
                                     Set<String> selectedRuns, RunModel run,
                                     TaskProvider<CreateMcpClientFilesTask> createMcpClientFilesTask) {
        if (!extension.getEnabled().get() || !selectedRuns.contains(run.getName())) {
            return;
        }

        File projectRoot = defaults.projectDirectory().getAsFile();
        File classOutput = defaults.classOutputDir().get().getAsFile();

        run.systemProperty("moddevmcp.project.root", projectRoot.getAbsolutePath());
        run.systemProperty("moddevmcp.compile.task", defaults.compileTaskPath().get());
        run.systemProperty("moddevmcp.class.output", classOutput.getAbsolutePath());
        run.systemProperty("moddevmcp.host", defaults.host().get());
        run.systemProperty("moddevmcp.port", String.valueOf(defaults.port().get()));
        project.getTasks().matching(task -> task.getName().equals(runTaskName(run.getName())))
                .configureEach(task -> task.dependsOn(createMcpClientFilesTask));
    }

    private static String runTaskName(String runName) {
        if (runName == null || runName.isBlank()) {
            return "run";
        }
        return "run" + Character.toUpperCase(runName.charAt(0)) + runName.substring(1);
    }
}
