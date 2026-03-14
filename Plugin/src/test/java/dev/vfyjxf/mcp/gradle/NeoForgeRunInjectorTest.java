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
    void injectsRuntimePropertiesIntoSelectedRunsWithoutJavaagent() {
        Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/plugin-consumer")).build();
        project.getPluginManager().apply("net.neoforged.moddev");
        project.getPluginManager().apply("dev.vfyjxf.moddevmcp");

        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.getRuns().create("client", RunModel::client);
        project.getTasks().register("runClient");
        evaluate(project);

        RunModel run = neoForge.getRuns().getByName("client");

        assertTrue(run.getJvmArguments().get().stream().noneMatch(argument -> argument.contains("-javaagent:")));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.project.root"));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.compile.task"));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.class.output"));
        assertEquals(":compileJava", run.getSystemProperties().get().get("moddevmcp.compile.task"));
        assertEquals(new File(project.getProjectDir(), "build/classes/java/main").getAbsolutePath(),
                run.getSystemProperties().get().get("moddevmcp.class.output"));
        assertTrue("127.0.0.1".equals(run.getSystemProperties().get().get("moddevmcp.host")));
        assertTrue("47653".equals(String.valueOf(run.getSystemProperties().get().get("moddevmcp.port"))));
        assertTrue(project.getTasks().getByName("runClient").getDependsOn().stream()
                .map(Object::toString)
                .anyMatch(value -> value.contains("createMcpClientFiles")));
    }

    @Test
    void derivesRuntimePropertiesWhenRunsAlreadyExist() {
        Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/plugin-consumer-existing-runs")).build();
        project.getPluginManager().apply("net.neoforged.moddev");

        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.getRuns().create("client", RunModel::client);

        project.getPluginManager().apply("dev.vfyjxf.moddevmcp");
        evaluate(project);

        RunModel run = neoForge.getRuns().getByName("client");

        assertTrue(run.getJvmArguments().get().stream().noneMatch(argument -> argument.contains("-javaagent:")));
        assertTrue(project.getProjectDir().getAbsolutePath().equals(run.getSystemProperties().get().get("moddevmcp.project.root")));
        assertTrue(":compileJava".equals(run.getSystemProperties().get().get("moddevmcp.compile.task")));
        assertTrue(new File(project.getProjectDir(), "build/classes/java/main").getAbsolutePath()
                .equals(run.getSystemProperties().get().get("moddevmcp.class.output")));
    }

    private static void evaluate(Project project) {
        ((ProjectInternal) project).evaluate();
    }
}
