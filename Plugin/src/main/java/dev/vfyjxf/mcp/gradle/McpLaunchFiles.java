package dev.vfyjxf.mcp.gradle;

import java.util.List;
import java.util.stream.Collectors;

public final class McpLaunchFiles {
    public static final String DEFAULT_CLASSPATH_FILE_NAME = "classpath.txt";
    public static final String DEFAULT_JAVA_ARGS_FILE_NAME = "mcp-server-java.args";
    public static final String DEFAULT_WINDOWS_SCRIPT_FILE_NAME = "run-mcp-server.bat";
    public static final String DEFAULT_POSIX_SCRIPT_FILE_NAME = "run-mcp-server.sh";

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
        return String.join("\r\n",
                "@echo off",
                "setlocal",
                "set SCRIPT_DIR=%~dp0",
                "set \"JAVA_CMD=java\"",
                "if defined JAVA_HOME if exist \"%JAVA_HOME%\\bin\\java.exe\" set \"JAVA_CMD=%JAVA_HOME%\\bin\\java.exe\"",
                "\"%JAVA_CMD%\" @\"%SCRIPT_DIR%" + argsFileName + "\" %*",
                ""
        );
    }

    public static String posixShellScript(String argsFileName) {
        return String.join("\n",
                "#!/usr/bin/env sh",
                "set -eu",
                "SCRIPT_DIR=$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)",
                "JAVA_CMD=java",
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

    public static String gooseSetupMarkdown(String serverId, String argsFileName) {
        return String.join(System.lineSeparator(),
                "# Goose MCP Setup",
                "",
                "Use Goose interactive MCP configuration and point it at this generated Java args file:",
                "",
                "- server id: `" + serverId + "`",
                "- command: `java`",
                "- args: `@" + argsFileName + "`",
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

    private static String quote(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    private static String tomlLiteral(String value) {
        return value.replace("'", "''");
    }

    private static String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }
}
