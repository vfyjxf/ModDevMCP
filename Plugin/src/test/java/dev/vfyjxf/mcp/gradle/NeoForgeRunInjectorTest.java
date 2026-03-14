package dev.vfyjxf.mcp.gradle;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeRunInjectorTest {

    @Test
    void injectsAgentAndRuntimePropertiesIntoSelectedRunsUsingResolvedMavenCoordinates() throws Exception {
        Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/plugin-consumer")).build();
        Path repoDir = Files.createTempDirectory("moddevmcp-agent-repo");
        Path jarPath = publishFakeAgent(repoDir, "dev.vfyjxf", "moddevmcp-agent", "1.2.3");

        project.getRepositories().maven(repository -> repository.setUrl(repoDir.toUri()));
        project.getPluginManager().apply("net.neoforged.moddev");
        project.getPluginManager().apply("dev.vfyjxf.moddevmcp");

        ModDevMcpExtension extension = project.getExtensions().getByType(ModDevMcpExtension.class);
        extension.getAgentCoordinates().set("dev.vfyjxf:moddevmcp-agent:1.2.3");
        extension.getMcpHost().set("127.0.0.1");
        extension.getMcpPort().set(47653);

        NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.getRuns().create("client", RunModel::client);
        project.getTasks().register("runClient");
        evaluate(project);

        RunModel run = neoForge.getRuns().getByName("client");

        assertTrue(run.getJvmArguments().get().contains("-javaagent:" + jarPath.toFile().getAbsolutePath()));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.project.root"));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.compile.task"));
        assertTrue(run.getSystemProperties().get().containsKey("moddevmcp.class.output"));
        assertTrue("127.0.0.1".equals(run.getSystemProperties().get().get("moddevmcp.host")));
        assertTrue("47653".equals(String.valueOf(run.getSystemProperties().get().get("moddevmcp.port"))));
        assertTrue(project.getTasks().getByName("runClient").getDependsOn().contains("createMcpClientFiles"));
    }

    @Test
    void usesConfiguredAgentJarPathOverride() {
        Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/plugin-consumer-agent-path")).build();
        project.getPluginManager().apply("net.neoforged.moddev");
        project.getPluginManager().apply("dev.vfyjxf.moddevmcp");

        ModDevMcpExtension extension = project.getExtensions().getByType(ModDevMcpExtension.class);
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

        project.getPluginManager().apply("dev.vfyjxf.moddevmcp");

        ModDevMcpExtension extension = project.getExtensions().getByType(ModDevMcpExtension.class);
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

    private static Path publishFakeAgent(Path repoDir, String group, String artifact, String version) throws IOException {
        Path artifactDir = repoDir.resolve(group.replace('.', File.separatorChar))
                .resolve(artifact)
                .resolve(version);
        Files.createDirectories(artifactDir);
        Path jarPath = artifactDir.resolve(artifact + "-" + version + ".jar");
        Path pomPath = artifactDir.resolve(artifact + "-" + version + ".pom");
        Files.write(jarPath, new byte[]{0});
        Files.writeString(pomPath, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </project>
                """.formatted(group, artifact, version));
        return jarPath;
    }
}
