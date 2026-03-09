package dev.vfyjxf.mcp.api;

import dev.vfyjxf.mcp.api.runtime.InputController;
import dev.vfyjxf.mcp.api.runtime.InventoryDriver;
import dev.vfyjxf.mcp.api.runtime.UiDriver;
import dev.vfyjxf.mcp.api.runtime.UiFramebufferCaptureProvider;
import dev.vfyjxf.mcp.api.runtime.UiInteractionStateResolver;
import dev.vfyjxf.mcp.api.runtime.UiOffscreenCaptureProvider;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

public final class ModMcpApi {

    private final RuntimeRegistries registries;

    public ModMcpApi(RuntimeRegistries registries) {
        this.registries = registries;
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
        registries.toolProviders().add(provider);
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
}
