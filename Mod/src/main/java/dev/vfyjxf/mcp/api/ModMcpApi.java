package dev.vfyjxf.mcp.api;

import dev.vfyjxf.mcp.api.event.EventPublisher;
import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.Objects;
import java.util.function.Consumer;

public final class ModMcpApi {

    private final RuntimeRegistries registries;
    private final Consumer<McpToolProvider> toolProviderSink;

    public ModMcpApi(RuntimeRegistries registries) {
        this(registries, provider -> registries.toolProviders().add(provider));
    }

    public ModMcpApi(RuntimeRegistries registries, Consumer<McpToolProvider> toolProviderSink) {
        this.registries = registries;
        this.toolProviderSink = Objects.requireNonNull(toolProviderSink, "toolProviderSink");
    }

    public void registerUiDriver(UiDriver driver) {
        registries.uiDrivers().register(driver);
    }

    public void registerInventoryDriver(InventoryDriver driver) {
        registries.inventoryDrivers().register(driver);
    }

    public void registerInputController(InputController controller) {
        registries.inputControllers().add(controller);
    }

    public void registerToolProvider(McpToolProvider provider) {
        toolProviderSink.accept(provider);
    }

    public void registerUiInteractionStateResolver(UiInteractionStateResolver resolver) {
        registries.uiInteractionResolvers().register(resolver);
    }

    public void registerUiOffscreenCaptureProvider(UiOffscreenCaptureProvider provider) {
        registries.uiOffscreenCaptureProviders().register(provider);
    }

    public void registerUiFramebufferCaptureProvider(UiFramebufferCaptureProvider provider) {
        registries.uiFramebufferCaptureProviders().register(provider);
    }

    public EventPublisher eventPublisher() {
        return registries.eventPublisher();
    }
}
