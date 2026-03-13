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
        this.mcpMainClass = objects.property(String.class).convention("dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain");
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
