package dev.vfyjxf.mcp.gradle;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.io.File;

public abstract class ModDevMcpHotswapExtension {

    private final Property<Boolean> enabled;
    private final Property<File> projectRoot;
    private final Property<String> agentJarPath;
    private final Property<String> compileTask;
    private final Property<String> classOutputDir;
    private final ListProperty<String> runs;
    private final Property<Boolean> requireEnhancedHotswap;

    @Inject
    public ModDevMcpHotswapExtension(ObjectFactory objects) {
        this.enabled = objects.property(Boolean.class).convention(true);
        this.projectRoot = objects.property(File.class);
        this.agentJarPath = objects.property(String.class);
        this.compileTask = objects.property(String.class).convention("compileJava");
        this.classOutputDir = objects.property(String.class).convention("build/classes/java/main");
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

    public Property<String> getCompileTask() {
        return compileTask;
    }

    public Property<String> getClassOutputDir() {
        return classOutputDir;
    }

    public ListProperty<String> getRuns() {
        return runs;
    }

    public Property<Boolean> getRequireEnhancedHotswap() {
        return requireEnhancedHotswap;
    }
}
