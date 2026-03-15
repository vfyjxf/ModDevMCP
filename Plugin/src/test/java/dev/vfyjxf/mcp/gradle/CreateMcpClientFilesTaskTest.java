package dev.vfyjxf.mcp.gradle;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateMcpClientFilesTaskTest {

    @Test
    void generateWritesDirectlyUsableCodexConfigWithResolvedJavaAndFixedEndpoint() throws Exception {
        var project = ProjectBuilder.builder().build();
        var task = project.getTasks().create("createMcpClientFiles", CreateMcpClientFilesTask.class);
        var outputDir = Files.createTempDirectory("moddevmcp-clients");
        var classpathEntry = Files.createTempFile(outputDir, "cp", ".jar");

        task.getServerId().set("moddevmcp");
        task.getMainClass().set("dev.vfyjxf.mcp.server.bootstrap.ModDevMcpGatewayMain");
        task.getBackendMainClass().set("dev.vfyjxf.mcp.server.bootstrap.ModDevMcpBackendMain");
        task.getRuntimeClasspath().from(classpathEntry.toFile());
        task.getOutputDir().set(outputDir.toFile());
        task.getJavaCommand().set("C:\\Program Files\\Zulu\\zulu-21\\bin\\java.exe");
        task.getMcpHost().set("127.0.0.1");
        task.getMcpPort().set(47653);
        task.getMcpProxyPort().set(47654);

        task.generate();

        var codexToml = Files.readString(outputDir.resolve("clients").resolve("codex.toml"));
        assertTrue(codexToml.contains("command = 'C:\\Program Files\\Zulu\\zulu-21\\bin\\java.exe'"));
        assertTrue(codexToml.contains("'-Dmoddevmcp.host=127.0.0.1'"));
        assertTrue(codexToml.contains("'-Dmoddevmcp.port=47653'"));
        assertTrue(codexToml.contains("'-Dmoddevmcp.mcpPort=47654'"));
        assertTrue(codexToml.contains("'-Dmoddevmcp.backend.javaCommand=C:\\Program Files\\Zulu\\zulu-21\\bin\\java.exe'"));
        assertTrue(codexToml.contains("'-Dmoddevmcp.backend.argsFile="
                + outputDir.resolve("mcp-backend-java.args").toAbsolutePath().toString().replace("'", "''") + "'"));
        assertTrue(codexToml.contains("'@" + outputDir.resolve("mcp-gateway-java.args").toAbsolutePath().toString().replace("'", "''") + "'"));

        assertTrue(Files.exists(outputDir.resolve("mcp-backend-java.args")));
        assertTrue(Files.exists(outputDir.resolve("mcp-gateway-java.args")));
        assertTrue(Files.exists(outputDir.resolve("run-mcp-backend.bat")));
        assertTrue(Files.exists(outputDir.resolve("run-mcp-gateway.bat")));
        assertTrue(Files.exists(outputDir.resolve("clients").resolve("claude-code.mcp.json")));
        assertTrue(Files.exists(outputDir.resolve("clients").resolve("cursor-mcp.json")));
        assertTrue(Files.exists(outputDir.resolve("clients").resolve("vscode-mcp.json")));
        assertTrue(Files.exists(outputDir.resolve("clients").resolve("gemini-settings.json")));
        assertFalse(Files.exists(outputDir.resolve("clients").resolve("mcp-servers.json")));
        assertFalse(Files.exists(outputDir.resolve("clients").resolve("claude-desktop.mcp.json")));
        assertFalse(Files.exists(outputDir.resolve("clients").resolve("cline_mcp_settings.json")));
        assertFalse(Files.exists(outputDir.resolve("clients").resolve("windsurf-mcp_config.json")));
        assertFalse(Files.exists(outputDir.resolve("clients").resolve("goose-setup.md")));
        assertTrue(Files.exists(outputDir.resolve("clients").resolve("INSTALL.md")));

        var installGuide = Files.readString(outputDir.resolve("clients").resolve("INSTALL.md"));
        assertTrue(installGuide.contains("Codex"));
        assertTrue(installGuide.contains("Claude Code"));
        assertTrue(installGuide.contains("Cursor"));
        assertTrue(installGuide.contains("VS Code"));
        assertTrue(installGuide.contains("Gemini CLI"));
    }
}
