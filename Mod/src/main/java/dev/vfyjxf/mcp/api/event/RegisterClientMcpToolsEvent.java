package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

public final class RegisterClientMcpToolsEvent extends RegisterMcpToolsEvent {

    public RegisterClientMcpToolsEvent(List<McpToolProvider> providers, ModMcpApi api, EventPublisher eventPublisher) {
        super(providers, api, eventPublisher);
    }

    public void registerUiDriver(UiDriver driver) {
        api().registerUiDriver(driver);
    }

    public void registerInventoryDriver(InventoryDriver driver) {
        api().registerInventoryDriver(driver);
    }

    public void registerInputController(InputController controller) {
        api().registerInputController(controller);
    }

    public void registerUiInteractionStateResolver(UiInteractionStateResolver resolver) {
        api().registerUiInteractionStateResolver(resolver);
    }

    public void registerUiOffscreenCaptureProvider(UiOffscreenCaptureProvider provider) {
        api().registerUiOffscreenCaptureProvider(provider);
    }

    public void registerUiFramebufferCaptureProvider(UiFramebufferCaptureProvider provider) {
        api().registerUiFramebufferCaptureProvider(provider);
    }
}
