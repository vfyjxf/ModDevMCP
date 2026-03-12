package dev.vfyjxf.gradle;

public final class EmbeddedMcpLaunchFiles {
    public static final String DEFAULT_CLASSPATH_FILE_NAME = "classpath.txt";
    public static final String DEFAULT_ARGS_FILE_NAME = "embedded-mcp-java.args";
    public static final String DEFAULT_BATCH_FILE_NAME = "run-embedded-mcp-stdio.bat";

    private EmbeddedMcpLaunchFiles() {
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

    public static String mcpClientTomlSnippet(String serverId, String command) {
        return String.join(System.lineSeparator(),
                "[mcp_servers." + serverId + "]",
                "command = '" + command + "'",
                ""
        );
    }

    private static String quote(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }
}
