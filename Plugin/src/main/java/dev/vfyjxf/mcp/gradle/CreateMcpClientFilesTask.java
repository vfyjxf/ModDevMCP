package dev.vfyjxf.mcp.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public abstract class CreateMcpClientFilesTask extends DefaultTask {

    private static final int BACKEND_START_TIMEOUT_MS = 60000;

    @Input
    public abstract Property<String> getServerId();

    @Input
    public abstract Property<String> getMainClass();

    @Input
    public abstract Property<String> getBackendMainClass();

    @Input
    public abstract Property<String> getJavaCommand();

    @Input
    public abstract Property<String> getMcpHost();

    @Input
    public abstract Property<Integer> getMcpPort();

    @Input
    public abstract Property<Integer> getMcpProxyPort();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() throws IOException {
        var outputDir = getOutputDir().get().getAsFile().toPath();
        Files.createDirectories(outputDir);
        deleteLegacyOutputs(outputDir);
        var clientsDir = outputDir.resolve("clients");
        Files.createDirectories(clientsDir);
        deleteLegacyClientOutputs(clientsDir);

        var classpath = getRuntimeClasspath().getAsPath();
        if (classpath == null || classpath.isBlank()) {
            throw new IllegalStateException("ModDevMCP runtime classpath must not be empty");
        }

        var classpathFile = outputDir.resolve(McpLaunchFiles.DEFAULT_CLASSPATH_FILE_NAME);
        var gatewayArgsFile = outputDir.resolve(McpLaunchFiles.DEFAULT_GATEWAY_JAVA_ARGS_FILE_NAME);
        var backendArgsFile = outputDir.resolve(McpLaunchFiles.DEFAULT_BACKEND_JAVA_ARGS_FILE_NAME);
        var gatewayWindowsScript = outputDir.resolve(McpLaunchFiles.DEFAULT_GATEWAY_WINDOWS_SCRIPT_FILE_NAME);
        var gatewayPosixScript = outputDir.resolve(McpLaunchFiles.DEFAULT_GATEWAY_POSIX_SCRIPT_FILE_NAME);
        var backendWindowsScript = outputDir.resolve(McpLaunchFiles.DEFAULT_BACKEND_WINDOWS_SCRIPT_FILE_NAME);
        var backendPosixScript = outputDir.resolve(McpLaunchFiles.DEFAULT_BACKEND_POSIX_SCRIPT_FILE_NAME);
        var backendLauncher = isWindows() ? backendWindowsScript : backendPosixScript;
        var gatewayArgsReference = "@" + gatewayArgsFile.toAbsolutePath();
        var clientArgs = List.of(
                "-Dmoddevmcp.host=" + getMcpHost().get(),
                "-Dmoddevmcp.port=" + getMcpPort().get(),
                "-Dmoddevmcp.mcpPort=" + getMcpProxyPort().get(),
                "-Dmoddevmcp.backend.javaCommand=" + getJavaCommand().get(),
                "-Dmoddevmcp.backend.argsFile=" + backendArgsFile.toAbsolutePath(),
                "-Dmoddevmcp.backend.launcher=" + backendLauncher.toAbsolutePath(),
                "-Dmoddevmcp.backend.startTimeoutMs=" + BACKEND_START_TIMEOUT_MS,
                gatewayArgsReference
        );

        Files.writeString(classpathFile, classpath);
        Files.writeString(gatewayArgsFile, McpLaunchFiles.javaArgs(classpath, getMainClass().get()));
        Files.writeString(backendArgsFile, McpLaunchFiles.javaArgs(classpath, getBackendMainClass().get()));
        Files.writeString(gatewayWindowsScript, McpLaunchFiles.windowsBatchScript(McpLaunchFiles.DEFAULT_GATEWAY_JAVA_ARGS_FILE_NAME, getJavaCommand().get()));
        Files.writeString(gatewayPosixScript, McpLaunchFiles.posixShellScript(McpLaunchFiles.DEFAULT_GATEWAY_JAVA_ARGS_FILE_NAME, getJavaCommand().get()));
        Files.writeString(backendWindowsScript, McpLaunchFiles.windowsBatchScript(McpLaunchFiles.DEFAULT_BACKEND_JAVA_ARGS_FILE_NAME, getJavaCommand().get()));
        Files.writeString(backendPosixScript, McpLaunchFiles.posixShellScript(McpLaunchFiles.DEFAULT_BACKEND_JAVA_ARGS_FILE_NAME, getJavaCommand().get()));
        gatewayPosixScript.toFile().setExecutable(true, false);
        backendPosixScript.toFile().setExecutable(true, false);

        writeClientFile(clientsDir.resolve("codex.toml"),
                McpLaunchFiles.mcpClientTomlSnippet(getServerId().get(), getJavaCommand().get(), clientArgs));
        writeClientFile(clientsDir.resolve("claude-code.mcp.json"),
                McpLaunchFiles.claudeCodeMcpJsonSnippet(getServerId().get(), getJavaCommand().get(), clientArgs));
        writeClientFile(clientsDir.resolve("cursor-mcp.json"),
                McpLaunchFiles.cursorMcpJsonSnippet(getServerId().get(), getJavaCommand().get(), clientArgs));
        writeClientFile(clientsDir.resolve("vscode-mcp.json"),
                McpLaunchFiles.vsCodeMcpJsonSnippet(getServerId().get(), getJavaCommand().get(), clientArgs));
        writeClientFile(clientsDir.resolve("gemini-settings.json"),
                McpLaunchFiles.geminiSettingsJsonSnippet(getServerId().get(), getJavaCommand().get(), clientArgs));
        writeClientFile(clientsDir.resolve(McpLaunchFiles.DEFAULT_INSTALL_GUIDE_FILE_NAME),
                McpLaunchFiles.agentInstallMarkdown(getServerId().get(), getJavaCommand().get(), clientArgs));
    }

    private static void writeClientFile(java.nio.file.Path path, String content) throws IOException {
        Files.writeString(path, content);
    }

    private static void deleteLegacyOutputs(java.nio.file.Path outputDir) throws IOException {
        Files.deleteIfExists(outputDir.resolve("mcp-server-java.args"));
        Files.deleteIfExists(outputDir.resolve("run-mcp-server.bat"));
        Files.deleteIfExists(outputDir.resolve("run-mcp-server.sh"));
    }

    private static void deleteLegacyClientOutputs(java.nio.file.Path clientsDir) throws IOException {
        Files.deleteIfExists(clientsDir.resolve("mcp-servers.json"));
        Files.deleteIfExists(clientsDir.resolve("claude-desktop.mcp.json"));
        Files.deleteIfExists(clientsDir.resolve("cline_mcp_settings.json"));
        Files.deleteIfExists(clientsDir.resolve("windsurf-mcp_config.json"));
        Files.deleteIfExists(clientsDir.resolve("goose-setup.md"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
