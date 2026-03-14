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
    public static final String DEFAULT_SHARED_JSON_FILE_NAME = "mcp-servers.json";
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

    public static String mcpServersJsonSnippet(String serverId, String command, List<String> args) {
        var lines = new java.util.ArrayList<String>();
        lines.add("{");
        lines.add("  \"mcpServers\": {");
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

    public static String gooseSetupMarkdown(String serverId, String command, List<String> args) {
        return String.join(System.lineSeparator(),
                "# Goose MCP Setup",
                "",
                "Use Goose interactive MCP configuration and point it at this generated MCP launch command:",
                "",
                "- server id: `" + serverId + "`",
                "- command: `" + command + "`",
                "- args: `" + String.join(" ", args) + "`",
                "",
                "Example:",
                "",
                "```text",
                "goose configure",
                "```",
                "",
                "Then enter the command and args shown above.",
                ""
        );
    }

    public static String agentInstallMarkdown(String serverId, String command, List<String> args) {
        var argsInline = args.stream()
                .map(value -> "`" + markdownLiteral(value) + "`")
                .collect(Collectors.joining(", "));
        return String.join(System.lineSeparator(),
                "# ModDevMCP Agent Install Guide",
                "",
                "Generated: 2026-03-14",
                "",
                "Use the generated files in this directory instead of rewriting the launch command by hand.",
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
                "- `mcp-servers.json`: shared JSON payload for clients that use `mcpServers`",
                "- `claude-code.mcp.json`: JSON snippet for Claude Code project/user config",
                "- `claude-desktop.mcp.json`: JSON snippet for Claude Desktop import/manual merge",
                "- `cursor-mcp.json`: JSON snippet for Cursor `mcp.json`",
                "- `cline_mcp_settings.json`: JSON snippet for Cline MCP settings",
                "- `windsurf-mcp_config.json`: JSON snippet for Windsurf `mcp_config.json`",
                "- `vscode-mcp.json`: JSON snippet for VS Code `.vscode/mcp.json`",
                "- `gemini-settings.json`: JSON snippet for Gemini CLI `settings.json`",
                "- `goose-setup.md`: Goose interactive install notes",
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
                "1. For a shared project config, merge `claude-code.mcp.json` into `<project>/.mcp.json`.",
                "2. For a private user/local install, add the same command with `claude mcp add --transport stdio " + serverId + " -- <command> <args...>`.",
                "3. Verify with `claude mcp list` or `/mcp`.",
                "",
                "### Cursor",
                "",
                "1. Merge `cursor-mcp.json` into `<project>/.cursor/mcp.json` or `~/.cursor/mcp.json`.",
                "2. If the new server does not appear immediately, refresh MCP settings or restart Cursor.",
                "",
                "### Cline",
                "",
                "1. Merge `cline_mcp_settings.json` into Cline MCP settings.",
                "2. In the extension UI, open `MCP Servers` -> `Configure` -> `Configure MCP Servers` if you prefer editing from the UI.",
                "",
                "### Windsurf",
                "",
                "1. Merge `windsurf-mcp_config.json` into `~/.codeium/windsurf/mcp_config.json`.",
                "2. In Cascade, refresh MCP servers after saving the config.",
                "",
                "### VS Code",
                "",
                "1. Merge `vscode-mcp.json` into `<project>/.vscode/mcp.json`.",
                "2. Reopen the workspace or refresh MCP if the tools list is stale.",
                "",
                "### Gemini CLI",
                "",
                "1. Merge `gemini-settings.json` into `~/.gemini/settings.json` or `<project>/.gemini/settings.json`.",
                "2. Or add it with `gemini mcp add` using the same command and args.",
                "",
                "### Goose",
                "",
                "1. Follow `goose-setup.md`.",
                "2. Prefer a command-line extension that runs the generated Java command and args directly.",
                "",
                "## Runtime Rule For Agents",
                "",
                "1. Start the MCP gateway first.",
                "2. Start Minecraft with `runClient` second.",
                "3. Call `moddev.status` first.",
                "4. Continue with UI tools only after `gameConnected=true`.",
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
