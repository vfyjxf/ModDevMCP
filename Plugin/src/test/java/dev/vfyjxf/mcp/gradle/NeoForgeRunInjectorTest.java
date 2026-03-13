package dev.vfyjxf.mcp.gradle;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeRunInjectorTest {

    @Test
    void injectsAgentAndRuntimePropertiesIntoSelectedRuns() {
        Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/plugin-consumer")).build();
        project.getPluginManager().apply("net.neoforged.moddev");
        project.getPluginManager().apply("dev.vfyjxf.moddev-hotswap");

        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.getRuns().create("client", RunModel::client);
        evaluate(project);

        RunModel run = neoForge.getRuns().getByName("client");

        assertTrue(run.getJvmArguments().get().stream().anyMatch(arg -> arg.startsWith("-javaagent:")));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.project.root"));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.compile.task"));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.class.output"));
        assertEquals("compileJava", run.getSystemProperties().get().get("moddevmcp.compile.task"));
        assertEquals(new File(project.getRootProject().getProjectDir(), "build/classes/java/main").getAbsolutePath(),
                run.getSystemProperties().get().get("moddevmcp.class.output"));
    }

    @Test
    void usesConfiguredAgentJarPathOverride() {
        Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/plugin-consumer-agent-path")).build();
        project.getPluginManager().apply("net.neoforged.moddev");
        project.getPluginManager().apply("dev.vfyjxf.moddev-hotswap");

        ModDevMcpHotswapExtension extension = project.getExtensions().getByType(ModDevMcpHotswapExtension.class);
        extension.getAgentJarPath().set("D:/external/Agent.jar");

        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.getRuns().create("client", RunModel::client);
        evaluate(project);

        RunModel run = neoForge.getRuns().getByName("client");

        assertTrue(run.getJvmArguments().get().contains("-javaagent:D:/external/Agent.jar"));
    }

    @Test
    void appliesExtensionOverridesWhenRunsAlreadyExist() {
        Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/plugin-consumer-existing-runs")).build();
        project.getPluginManager().apply("net.neoforged.moddev");

        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.getRuns().create("client", RunModel::client);

        project.getPluginManager().apply("dev.vfyjxf.moddev-hotswap");

        ModDevMcpHotswapExtension extension = project.getExtensions().getByType(ModDevMcpHotswapExtension.class);
        File projectRoot = new File("D:/workspace/TestMod");
        extension.getProjectRoot().set(projectRoot);
        extension.getAgentJarPath().set("D:/external/Agent.jar");
        extension.getCompileTask().set(":compileJava");
        extension.getClassOutputDir().set("build/classes/java/main");
        evaluate(project);

        RunModel run = neoForge.getRuns().getByName("client");

        assertTrue(run.getJvmArguments().get().contains("-javaagent:D:/external/Agent.jar"));
        assertTrue(projectRoot.getAbsolutePath().equals(run.getSystemProperties().get().get("moddevmcp.project.root")));
        assertTrue(":compileJava".equals(run.getSystemProperties().get().get("moddevmcp.compile.task")));
        assertTrue(new File(projectRoot, "build/classes/java/main").getAbsolutePath()
                .equals(run.getSystemProperties().get().get("moddevmcp.class.output")));
    }

    private static void evaluate(Project project) {
        ((ProjectInternal) project).evaluate();
    }
}
