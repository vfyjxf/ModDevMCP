package dev.vfyjxf.mcp.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModDevMcpPluginTest {

    @Test
    void pluginIdCanBeAppliedAndCreatesExtension() {
        Project project = ProjectBuilder.builder().build();

        assertDoesNotThrow(() -> project.getPluginManager().apply("dev.vfyjxf.moddevmcp"));

        assertNotNull(project.getExtensions().findByName("modDevMcp"));
    }
}
