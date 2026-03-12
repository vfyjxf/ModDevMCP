package dev.vfyjxf.mcp.gradle;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeRunInjectorTest {

    @Test
    void injectsAgentAndRuntimePropertiesIntoSelectedRuns() {
        Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/plugin-consumer")).build();
        project.getPluginManager().apply("net.neoforged.moddev");
        project.getPluginManager().apply("dev.vfyjxf.moddev-hotswap");

        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.getRuns().create("client", RunModel::client);

        RunModel run = neoForge.getRuns().getByName("client");

        assertTrue(run.getJvmArguments().get().stream().anyMatch(arg -> arg.startsWith("-javaagent:")));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.project.root"));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.compile.task"));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.class.output"));
    }
}
