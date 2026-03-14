package dev.vfyjxf.mcp.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDevMcpPluginTest {

    @Test
    void pluginIdCanBeAppliedAndCreatesExtension() {
        Project project = ProjectBuilder.builder().build();

        assertDoesNotThrow(() -> project.getPluginManager().apply("dev.vfyjxf.moddevmcp"));

        assertNotNull(project.getExtensions().findByName("modDevMcp"));
    }

    @Test
    void pluginDefaultsMcpRuntimeClasspathFromPublishedServerArtifact() throws Exception {
        Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/plugin-consumer-runtime")).build();
        Path repoDir = Files.createTempDirectory("moddevmcp-server-repo");
        Path runtimeLibJar = publishFakeModule(repoDir, "example", "runtime-lib", "9.0", List.of());
        Path serverJar = publishFakeModule(
                repoDir,
                "dev.vfyjxf",
                "moddevmcp-server",
                "0.1.1",
                List.of(new ModuleDependency("example", "runtime-lib", "9.0"))
        );

        project.getRepositories().maven(repository -> repository.setUrl(repoDir.toUri()));
        project.getPluginManager().apply("dev.vfyjxf.moddevmcp");

        CreateMcpClientFilesTask task = (CreateMcpClientFilesTask) project.getTasks().getByName("createMcpClientFiles");

        assertTrue(task.getRuntimeClasspath().getFiles().contains(serverJar.toFile()));
        assertTrue(task.getRuntimeClasspath().getFiles().contains(runtimeLibJar.toFile()));
    }

    @Test
    void extensionKeepsOnlyMinimalPublicDslSurface() {
        Set<String> getterNames = java.util.Arrays.stream(ModDevMcpExtension.class.getMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("get"))
                .collect(Collectors.toSet());

        assertTrue(getterNames.contains("getEnabled"));
        assertTrue(getterNames.contains("getRuns"));
        assertTrue(getterNames.contains("getRequireEnhancedHotswap"));

        assertTrue(!getterNames.contains("getAgentVersion"));
        assertTrue(!getterNames.contains("getAgentJarPath"));
        assertTrue(!getterNames.contains("getProjectRoot"));
        assertTrue(!getterNames.contains("getCompileTask"));
        assertTrue(!getterNames.contains("getClassOutputDir"));
        assertTrue(!getterNames.contains("getMcpMainClass"));
        assertTrue(!getterNames.contains("getBackendMainClass"));
        assertTrue(!getterNames.contains("getJavaCommand"));
        assertTrue(!getterNames.contains("getMcpHost"));
        assertTrue(!getterNames.contains("getMcpPort"));
        assertTrue(!getterNames.contains("getMcpProxyPort"));
        assertTrue(!getterNames.contains("getMcpRuntimeClasspath"));
        assertTrue(!getterNames.contains("getMcpClientFilesOutputDir"));
    }

    private static Path publishFakeModule(Path repoDir, String group, String artifact, String version,
                                          List<ModuleDependency> dependencies) throws IOException {
        Path artifactDir = repoDir.resolve(group.replace('.', File.separatorChar))
                .resolve(artifact)
                .resolve(version);
        Files.createDirectories(artifactDir);
        Path jarPath = artifactDir.resolve(artifact + "-" + version + ".jar");
        Path pomPath = artifactDir.resolve(artifact + "-" + version + ".pom");
        Files.write(jarPath, new byte[]{0});
        Files.writeString(pomPath, pom(group, artifact, version, dependencies));
        return jarPath;
    }

    private static String pom(String group, String artifact, String version, List<ModuleDependency> dependencies) {
        String dependencyXml = dependencies.stream()
                .map(dependency -> """
                        <dependency>
                          <groupId>%s</groupId>
                          <artifactId>%s</artifactId>
                          <version>%s</version>
                        </dependency>
                        """.formatted(dependency.group(), dependency.artifact(), dependency.version()))
                .reduce("", String::concat);
        String dependenciesBlock = dependencies.isEmpty() ? "" : "<dependencies>\n" + dependencyXml + "</dependencies>\n";
        return """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                  %s
                </project>
                """.formatted(group, artifact, version, dependenciesBlock);
    }

    private record ModuleDependency(String group, String artifact, String version) {
    }
}
