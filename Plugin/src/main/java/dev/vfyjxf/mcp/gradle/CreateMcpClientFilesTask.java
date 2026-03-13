package dev.vfyjxf.mcp.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public abstract class CreateMcpClientFilesTask extends DefaultTask {

    @Input
    public abstract Property<String> getServerId();

    @Input
    public abstract Property<String> getMainClass();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() throws IOException {
        var outputDir = getOutputDir().get().getAsFile().toPath();
        Files.createDirectories(outputDir);
        var clientsDir = outputDir.resolve("clients");
        Files.createDirectories(clientsDir);

        var classpath = getRuntimeClasspath().getAsPath();
        if (classpath == null || classpath.isBlank()) {
            throw new IllegalStateException("mcpRuntimeClasspath must not be empty");
        }

        var classpathFile = outputDir.resolve(McpLaunchFiles.DEFAULT_CLASSPATH_FILE_NAME);
        var argsFile = outputDir.resolve(McpLaunchFiles.DEFAULT_JAVA_ARGS_FILE_NAME);
        var windowsScript = outputDir.resolve(McpLaunchFiles.DEFAULT_WINDOWS_SCRIPT_FILE_NAME);
        var posixScript = outputDir.resolve(McpLaunchFiles.DEFAULT_POSIX_SCRIPT_FILE_NAME);
        var argsReference = "@" + argsFile.toAbsolutePath();

        Files.writeString(classpathFile, classpath);
        Files.writeString(argsFile, McpLaunchFiles.javaArgs(classpath, getMainClass().get()));
        Files.writeString(windowsScript, McpLaunchFiles.windowsBatchScript(McpLaunchFiles.DEFAULT_JAVA_ARGS_FILE_NAME));
        Files.writeString(posixScript, McpLaunchFiles.posixShellScript(McpLaunchFiles.DEFAULT_JAVA_ARGS_FILE_NAME));
        posixScript.toFile().setExecutable(true, false);

        writeClientFile(clientsDir.resolve("codex.toml"),
                McpLaunchFiles.mcpClientTomlSnippet(getServerId().get(), "java", List.of(argsReference)));
        var mcpServersJson = McpLaunchFiles.mcpServersJsonSnippet(getServerId().get(), "java", List.of(argsReference));
        writeClientFile(clientsDir.resolve("claude-code.mcp.json"), mcpServersJson);
        writeClientFile(clientsDir.resolve("cursor-mcp.json"), mcpServersJson);
        writeClientFile(clientsDir.resolve("cline_mcp_settings.json"), mcpServersJson);
        writeClientFile(clientsDir.resolve("windsurf-mcp_config.json"), mcpServersJson);
        writeClientFile(clientsDir.resolve("vscode-mcp.json"), mcpServersJson);
        writeClientFile(clientsDir.resolve("gemini-settings.json"), mcpServersJson);
        writeClientFile(clientsDir.resolve("goose-setup.md"),
                McpLaunchFiles.gooseSetupMarkdown(getServerId().get(), argsFile.toAbsolutePath().toString()));
    }

    private static void writeClientFile(java.nio.file.Path path, String content) throws IOException {
        Files.writeString(path, content);
    }
}
