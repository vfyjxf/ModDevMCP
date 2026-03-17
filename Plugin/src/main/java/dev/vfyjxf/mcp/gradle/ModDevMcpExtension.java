package dev.vfyjxf.mcp.gradle;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class ModDevMcpExtension {

    private final Property<Boolean> enabled;
    private final ListProperty<String> runs;
    private final Property<Boolean> requireEnhancedHotswap;
    private final Property<String> serverVersion;

    @Inject
    public ModDevMcpExtension(ObjectFactory objects) {
        this.enabled = objects.property(Boolean.class).convention(true);
        this.runs = objects.listProperty(String.class).convention(java.util.List.of("client"));
        this.requireEnhancedHotswap = objects.property(Boolean.class).convention(false);
        this.serverVersion = objects.property(String.class);
    }

    public Property<Boolean> getEnabled() {
        return enabled;
    }

    public ListProperty<String> getRuns() {
        return runs;
    }

    public Property<Boolean> getRequireEnhancedHotswap() {
        return requireEnhancedHotswap;
    }

    public Property<String> getServerVersion() {
        return serverVersion;
    }
}
