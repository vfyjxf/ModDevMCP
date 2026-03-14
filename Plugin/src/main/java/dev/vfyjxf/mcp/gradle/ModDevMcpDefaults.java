package dev.vfyjxf.mcp.gradle;

import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;

public record ModDevMcpDefaults(
        Directory projectDirectory,
        Provider<String> compileTaskPath,
        Provider<Directory> classOutputDir,
        Provider<String> serverId,
        Provider<String> gatewayMainClass,
        Provider<String> backendMainClass,
        Provider<String> javaCommand,
        Provider<String> host,
        Provider<Integer> port,
        Provider<Integer> proxyPort,
        FileCollection runtimeClasspath,
        Provider<Directory> clientFilesOutputDir
) {
}
