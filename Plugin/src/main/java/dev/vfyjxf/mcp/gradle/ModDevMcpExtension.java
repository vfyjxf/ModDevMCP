package dev.vfyjxf.mcp.gradle;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.io.File;

public abstract class ModDevMcpExtension {

    private final Property<Boolean> enabled;
    private final Property<File> projectRoot;
    private final Property<String> agentJarPath;
    private final Property<String> agentCoordinates;
    private final Property<String> compileTask;
    private final Property<String> classOutputDir;
    private final Property<String> mcpServerId;
    private final Property<String> mcpMainClass;
    private final Property<String> backendMainClass;
    private final Property<String> javaCommand;
    private final Property<String> mcpHost;
    private final Property<Integer> mcpPort;
    private final Property<Integer> mcpProxyPort;
    private final ConfigurableFileCollection mcpRuntimeClasspath;
    private final DirectoryProperty mcpClientFilesOutputDir;
    private final ListProperty<String> runs;
    private final Property<Boolean> requireEnhancedHotswap;

    @Inject
    public ModDevMcpExtension(ObjectFactory objects) {
        this.enabled = objects.property(Boolean.class).convention(true);
        this.projectRoot = objects.property(File.class);
        this.agentJarPath = objects.property(String.class);
        this.agentCoordinates = objects.property(String.class);
        this.compileTask = objects.property(String.class).convention(":Mod:compileJava");
        this.classOutputDir = objects.property(String.class).convention("Mod/build/classes/java/main");
        this.mcpServerId = objects.property(String.class).convention("moddevmcp");
        this.mcpMainClass = objects.property(String.class).convention("dev.vfyjxf.mcp.server.bootstrap.ModDevMcpGatewayMain");
        this.backendMainClass = objects.property(String.class).convention("dev.vfyjxf.mcp.server.bootstrap.ModDevMcpBackendMain");
        this.javaCommand = objects.property(String.class);
        this.mcpHost = objects.property(String.class).convention("127.0.0.1");
        this.mcpPort = objects.property(Integer.class).convention(47653);
        this.mcpProxyPort = objects.property(Integer.class).convention(47654);
        this.mcpRuntimeClasspath = objects.fileCollection();
        this.mcpClientFilesOutputDir = objects.directoryProperty();
        this.runs = objects.listProperty(String.class).convention(java.util.List.of("client"));
        this.requireEnhancedHotswap = objects.property(Boolean.class).convention(false);
    }

    public Property<Boolean> getEnabled() {
        return enabled;
    }

    public Property<File> getProjectRoot() {
        return projectRoot;
    }

    public Property<String> getAgentJarPath() {
        return agentJarPath;
    }

    public Property<String> getAgentCoordinates() {
        return agentCoordinates;
    }

    public Property<String> getCompileTask() {
        return compileTask;
    }

    public Property<String> getClassOutputDir() {
        return classOutputDir;
    }

    public Property<String> getMcpServerId() {
        return mcpServerId;
    }

    public Property<String> getMcpMainClass() {
        return mcpMainClass;
    }

    public Property<String> getJavaCommand() {
        return javaCommand;
    }

    public Property<String> getBackendMainClass() {
        return backendMainClass;
    }

    public Property<String> getMcpHost() {
        return mcpHost;
    }

    public Property<Integer> getMcpPort() {
        return mcpPort;
    }

    public Property<Integer> getMcpProxyPort() {
        return mcpProxyPort;
    }

    public ConfigurableFileCollection getMcpRuntimeClasspath() {
        return mcpRuntimeClasspath;
    }

    public DirectoryProperty getMcpClientFilesOutputDir() {
        return mcpClientFilesOutputDir;
    }

    public ListProperty<String> getRuns() {
        return runs;
    }

    public Property<Boolean> getRequireEnhancedHotswap() {
        return requireEnhancedHotswap;
    }
}
