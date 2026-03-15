package dev.vfyjxf.mcp.gradle;

import java.util.List;
import java.util.stream.Collectors;

public final class McpLaunchFiles {
    public static final String DEFAULT_CLASSPATH_FILE_NAME = "classpath.txt";
    public static final String DEFAULT_GATEWAY_JAVA_ARGS_FILE_NAME = "mcp-gateway-java.args";
    public static final String DEFAULT_BACKEND_JAVA_ARGS_FILE_NAME = "mcp-backend-java.args";
    public static final String DEFAULT_GATEWAY_WINDOWS_SCRIPT_FILE_NAME = "run-mcp-gateway.bat";
    public static final String DEFAULT_GATEWAY_POSIX_SCRIPT_FILE_NAME = "run-mcp-gateway.sh";
    public static final String DEFAULT_BACKEND_WINDOWS_SCRIPT_FILE_NAME = "run-mcp-backend.bat";
    public static final String DEFAULT_BACKEND_POSIX_SCRIPT_FILE_NAME = "run-mcp-backend.sh";
    public static final String DEFAULT_INSTALL_GUIDE_FILE_NAME = "INSTALL.md";

    private McpLaunchFiles() {
    }

    public static String javaArgs(String classpath, String mainClass) {
        return String.join(System.lineSeparator(),
                "-cp",
                quote(classpath),
                mainClass,
                ""
        );
    }

    public static String windowsBatchScript(String argsFileName) {
        return windowsBatchScript(argsFileName, "java");
    }

    public static String windowsBatchScript(String argsFileName, String javaCommand) {
        return String.join("\r\n",
                "@echo off",
                "setlocal",
                "set SCRIPT_DIR=%~dp0",
                "set \"JAVA_CMD=" + windowsBatchLiteral(javaCommand) + "\"",
                "if not exist \"%JAVA_CMD%\" set \"JAVA_CMD=java\"",
                "if defined JAVA_HOME if exist \"%JAVA_HOME%\\bin\\java.exe\" set \"JAVA_CMD=%JAVA_HOME%\\bin\\java.exe\"",
                "\"%JAVA_CMD%\" @\"%SCRIPT_DIR%" + argsFileName + "\" %*",
                ""
        );
    }

    public static String posixShellScript(String argsFileName) {
        return posixShellScript(argsFileName, "java");
    }

    public static String posixShellScript(String argsFileName, String javaCommand) {
        return String.join("\n",
                "#!/usr/bin/env sh",
                "set -eu",
                "SCRIPT_DIR=$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)",
                "JAVA_CMD=\"" + shellLiteral(javaCommand) + "\"",
                "if [ ! -x \"$JAVA_CMD\" ]; then JAVA_CMD=java; fi",
                "if [ -n \"${JAVA_HOME:-}\" ] && [ -x \"$JAVA_HOME/bin/java\" ]; then JAVA_CMD=\"$JAVA_HOME/bin/java\"; fi",
                "\"$JAVA_CMD\" @\"$SCRIPT_DIR/" + argsFileName + "\" \"$@\"",
                ""
        );
    }

    public static String mcpClientTomlSnippet(String serverId, String command) {
        return mcpClientTomlSnippet(serverId, command, List.of());
    }

    public static String mcpClientTomlSnippet(String serverId, String command, List<String> args) {
        var lines = new java.util.ArrayList<String>();
        lines.add("[mcp_servers." + serverId + "]");
        lines.add("command = '" + tomlLiteral(command) + "'");
        if (!args.isEmpty()) {
            lines.add("args = [");
            lines.addAll(args.stream()
                    .map(value -> "  '" + tomlLiteral(value) + "',")
                    .collect(Collectors.toList()));
            lines.add("]");
        }
        lines.add("");
        return String.join(System.lineSeparator(), lines);
    }

    public static String claudeCodeMcpJsonSnippet(String serverId, String command, List<String> args) {
        return commandJsonSnippet("mcpServers", serverId, command, args);
    }

    public static String cursorMcpJsonSnippet(String serverId, String command, List<String> args) {
        return commandJsonSnippet("mcpServers", serverId, command, args);
    }

    public static String vsCodeMcpJsonSnippet(String serverId, String command, List<String> args) {
        return commandJsonSnippet("servers", serverId, command, args);
    }

    public static String geminiSettingsJsonSnippet(String serverId, String command, List<String> args) {
        return commandJsonSnippet("mcpServers", serverId, command, args);
    }

    private static String commandJsonSnippet(String rootKey, String serverId, String command, List<String> args) {
        var lines = new java.util.ArrayList<String>();
        lines.add("{");
        lines.add("  " + jsonString(rootKey) + ": {");
        lines.add("    " + jsonString(serverId) + ": {");
        lines.add("      \"command\": " + jsonString(command) + ",");
        if (args.isEmpty()) {
            lines.add("      \"args\": []");
        } else {
            lines.add("      \"args\": [");
            for (int index = 0; index < args.size(); index++) {
                var suffix = index + 1 == args.size() ? "" : ",";
                lines.add("        " + jsonString(args.get(index)) + suffix);
            }
            lines.add("      ]");
        }
        lines.add("    }");
        lines.add("  }");
        lines.add("}");
        lines.add("");
        return String.join(System.lineSeparator(), lines);
    }

    public static String agentInstallMarkdown(String serverId, String command, List<String> args) {
        var argsInline = args.stream()
                .map(value -> "`" + markdownLiteral(value) + "`")
                .collect(Collectors.joining(", "));
        return String.join(System.lineSeparator(),
                "# ModDevMCP Agent Install Guide",
                "",
                "Generated: 2026-03-15",
                "",
                "Use the generated files in this directory instead of rewriting the launch command by hand.",
                "Only clients with an officially verified config format are emitted here.",
                "",
                "## Shared Launch Command",
                "",
                "- server id: `" + serverId + "`",
                "- command: `" + markdownLiteral(command) + "`",
                "- args: " + argsInline,
                "",
                "## Generated Files",
                "",
                "- `codex.toml`: snippet for Codex `~/.codex/config.toml`",
                "- `claude-code.mcp.json`: JSON snippet for Claude Code project `.mcp.json`",
                "- `cursor-mcp.json`: JSON snippet for Cursor `.cursor/mcp.json`",
                "- `vscode-mcp.json`: JSON snippet for VS Code `.vscode/mcp.json`",
                "- `gemini-settings.json`: JSON snippet for Gemini CLI `.gemini/settings.json`",
                "",
                "## Install By Agent",
                "",
                "### Codex",
                "",
                "1. Merge `codex.toml` into `~/.codex/config.toml`, or add the same server with `codex mcp add`.",
                "2. Verify with `codex mcp list`.",
                "",
                "### Claude Code",
                "",
                "1. Merge `claude-code.mcp.json` into `<project>/.mcp.json`.",
                "2. For a user-scoped install, add the same command with `claude mcp add --transport stdio " + serverId + " -- <command> <args...>`.",
                "3. Verify with `claude mcp list` or `/mcp`.",
                "",
                "### Cursor",
                "",
                "1. Merge `cursor-mcp.json` into `<project>/.cursor/mcp.json`.",
                "2. If the new server does not appear immediately, refresh MCP settings or restart Cursor.",
                "",
                "### VS Code",
                "",
                "1. Merge `vscode-mcp.json` into `<project>/.vscode/mcp.json`.",
                "2. Reopen the workspace or refresh MCP if the tools list is stale.",
                "",
                "### Gemini CLI",
                "",
                "1. Merge `gemini-settings.json` into `<project>/.gemini/settings.json` or `~/.gemini/settings.json`.",
                "2. Or add it with `gemini mcp add` using the same command and args.",
                "",
                "## Runtime Rule For Agents",
                "",
                "1. Start the MCP gateway first.",
                "2. Start Minecraft with `runClient` second.",
                "3. Call `moddev.status` first.",
                "4. Continue with UI tools only after `gameConnected=true`.",
                "",
                "## Not Generated On Purpose",
                "",
                "Clients without a freshly verified official config format are not emitted by this task.",
                ""
        );
    }

    private static String quote(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    private static String windowsBatchLiteral(String value) {
        return value.replace("^", "^^").replace("%", "%%").replace("\"", "\\\"");
    }

    private static String shellLiteral(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String tomlLiteral(String value) {
        return value.replace("'", "''");
    }

    private static String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    private static String markdownLiteral(String value) {
        return value.replace("`", "\\`");
    }
}
