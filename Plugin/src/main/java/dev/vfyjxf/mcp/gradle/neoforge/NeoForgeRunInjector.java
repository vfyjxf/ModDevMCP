package dev.vfyjxf.mcp.gradle.neoforge;

import dev.vfyjxf.mcp.gradle.ModDevMcpHotswapExtension;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.Project;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class NeoForgeRunInjector {

    public void inject(Project project, ModDevMcpHotswapExtension extension) {
        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        Set<String> selectedRuns = new HashSet<>(extension.getRuns().get());
        neoForge.getRuns().configureEach(run -> configureRun(project, extension, selectedRuns, run));
    }

    private static void configureRun(Project project, ModDevMcpHotswapExtension extension,
                                     Set<String> selectedRuns, RunModel run) {
        if (!extension.getEnabled().get() || !selectedRuns.contains(run.getName())) {
            return;
        }

        File projectRoot = extension.getProjectRoot().get();
        File classOutput = resolvePath(projectRoot, extension.getClassOutputDir().get());
        String agentPath = extension.getAgentJarPath().isPresent()
                ? extension.getAgentJarPath().get()
                : project.getRootProject().file("Agent/build/libs/Agent.jar").getAbsolutePath();

        run.jvmArgument("-javaagent:" + agentPath);
        run.systemProperty("moddevmcp.project.root", projectRoot.getAbsolutePath());
        run.systemProperty("moddevmcp.compile.task", extension.getCompileTask().get());
        run.systemProperty("moddevmcp.class.output", classOutput.getAbsolutePath());
    }

    private static File resolvePath(File projectRoot, String configuredPath) {
        File path = new File(configuredPath);
        if (path.isAbsolute()) {
            return path;
        }
        return new File(projectRoot, configuredPath);
    }
}
