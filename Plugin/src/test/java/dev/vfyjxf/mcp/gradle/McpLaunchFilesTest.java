package dev.vfyjxf.mcp.gradle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpLaunchFilesTest {

    @Test
    void javaArgsFileCarriesQuotedClasspathAndMainClass() {
        var content = McpLaunchFiles.javaArgs(
                "C:\\libs\\alpha.jar;C:\\Program Files\\Java\\beta.jar",
                "dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain"
        );

        assertTrue(content.contains("-cp"));
        assertTrue(content.contains("\"C:\\\\libs\\\\alpha.jar;C:\\\\Program Files\\\\Java\\\\beta.jar\""));
        assertTrue(content.contains("dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain"));
    }

    @Test
    void batchScriptUsesArgumentFileInsteadOfInlineClasspathVariable() {
        var script = McpLaunchFiles.windowsBatchScript("mcp-server-java.args", "C:\\Program Files\\Zulu\\zulu-21\\bin\\java.exe");

        assertTrue(script.contains("set \"JAVA_CMD=C:\\Program Files\\Zulu\\zulu-21\\bin\\java.exe\""));
        assertTrue(script.contains("if not exist \"%JAVA_CMD%\" set \"JAVA_CMD=java\""));
        assertTrue(script.contains("if defined JAVA_HOME if exist \"%JAVA_HOME%\\bin\\java.exe\" set \"JAVA_CMD=%JAVA_HOME%\\bin\\java.exe\""));
        assertTrue(script.contains("\"%JAVA_CMD%\" @\"%SCRIPT_DIR%mcp-server-java.args\" %*"));
        assertFalse(script.contains("set /p MODDEVMCP_CLASSPATH"));
        assertFalse(script.contains("-cp \"%MODDEVMCP_CLASSPATH%\""));
    }

    @Test
    void posixScriptUsesJavaHomeWhenAvailable() {
        var script = McpLaunchFiles.posixShellScript("mcp-server-java.args", "/opt/java/bin/java");

        assertTrue(script.contains("#!/usr/bin/env sh"));
        assertTrue(script.contains("JAVA_CMD=\"/opt/java/bin/java\""));
        assertTrue(script.contains("if [ ! -x \"$JAVA_CMD\" ]; then JAVA_CMD=java; fi"));
        assertTrue(script.contains("$JAVA_HOME/bin/java"));
        assertTrue(script.contains("\"$JAVA_CMD\" @\"$SCRIPT_DIR/mcp-server-java.args\" \"$@\""));
    }

    @Test
    void mcpClientTomlSnippetCanEmitCommandAndArgs() {
        var snippet = McpLaunchFiles.mcpClientTomlSnippet(
                "moddevmcp",
                "java",
                java.util.List.of("-cp", "<classpath>", "dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain")
        );

        assertTrue(snippet.contains("[mcp_servers.moddevmcp]"));
        assertTrue(snippet.contains("command = 'java'"));
        assertTrue(snippet.contains("args = ["));
        assertTrue(snippet.contains("'<classpath>'"));
        assertTrue(snippet.contains("'dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain'"));
    }

    @Test
    void mcpServersJsonSnippetCanEmitCommandAndArgs() {
        var snippet = McpLaunchFiles.mcpServersJsonSnippet(
                "moddevmcp",
                "java",
                java.util.List.of("@mcp-server-java.args")
        );

        assertTrue(snippet.contains("\"mcpServers\""));
        assertTrue(snippet.contains("\"moddevmcp\""));
        assertTrue(snippet.contains("\"command\": \"java\""));
        assertTrue(snippet.contains("\"args\": ["));
        assertTrue(snippet.contains("\"@mcp-server-java.args\""));
    }

    @Test
    void agentInstallMarkdownMentionsMainstreamClients() {
        var markdown = McpLaunchFiles.agentInstallMarkdown(
                "moddevmcp",
                "java",
                java.util.List.of("@mcp-gateway-java.args")
        );

        assertTrue(markdown.contains("Codex"));
        assertTrue(markdown.contains("Claude Code"));
        assertTrue(markdown.contains("Cursor"));
        assertTrue(markdown.contains("Cline"));
        assertTrue(markdown.contains("Windsurf"));
        assertTrue(markdown.contains("VS Code"));
        assertTrue(markdown.contains("Gemini CLI"));
        assertTrue(markdown.contains("Goose"));
    }
}
