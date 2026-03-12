package dev.vfyjxf.gradle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedMcpLaunchFilesTest {

    @Test
    void javaArgsFileCarriesQuotedClasspathAndMainClass() {
        var content = EmbeddedMcpLaunchFiles.javaArgs(
                "C:\\libs\\alpha.jar;C:\\Program Files\\Java\\beta.jar",
                "dev.vfyjxf.mcp.bootstrap.EmbeddedModDevMcpStdioMain"
        );

        assertTrue(content.contains("-cp"));
        assertTrue(content.contains("\"C:\\\\libs\\\\alpha.jar;C:\\\\Program Files\\\\Java\\\\beta.jar\""));
        assertTrue(content.contains("dev.vfyjxf.mcp.bootstrap.EmbeddedModDevMcpStdioMain"));
    }

    @Test
    void batchScriptUsesArgumentFileInsteadOfInlineClasspathVariable() {
        var script = EmbeddedMcpLaunchFiles.windowsBatchScript("embedded-mcp-java.args");

        assertTrue(script.contains("set \"JAVA_CMD=java\""));
        assertTrue(script.contains("if defined JAVA_HOME if exist \"%JAVA_HOME%\\bin\\java.exe\" set \"JAVA_CMD=%JAVA_HOME%\\bin\\java.exe\""));
        assertTrue(script.contains("\"%JAVA_CMD%\" @\"%SCRIPT_DIR%embedded-mcp-java.args\" %*"));
        assertFalse(script.contains("set /p MODDEVMCP_CLASSPATH"));
        assertFalse(script.contains("-cp \"%MODDEVMCP_CLASSPATH%\""));
    }

    @Test
    void mcpClientTomlSnippetUsesCommandOnlyServerDefinition() {
        var snippet = EmbeddedMcpLaunchFiles.mcpClientTomlSnippet(
                "moddevmcp",
                "D:\\ProjectDir\\AgentFarm\\ModDevMCP\\Mod\\build\\moddevmcp\\bridge-mcp\\run-bridge-mcp-stdio.bat"
        );

        assertTrue(snippet.contains("[mcp_servers.moddevmcp]"));
        assertTrue(snippet.contains("command = 'D:\\ProjectDir\\AgentFarm\\ModDevMCP\\Mod\\build\\moddevmcp\\bridge-mcp\\run-bridge-mcp-stdio.bat'"));
        assertFalse(snippet.contains("args = ["));
    }
}
