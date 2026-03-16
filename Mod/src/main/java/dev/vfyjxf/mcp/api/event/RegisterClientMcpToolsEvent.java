package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.List;

/**
 * Client-side registrar event used to register tool providers and client runtime adapters.
 */
public final class RegisterClientMcpToolsEvent extends RegisterMcpToolsEvent {

    public RegisterClientMcpToolsEvent(List<McpToolProvider> providers, ModMcpApi api, EventPublisher eventPublisher) {
        super(providers, api, eventPublisher);
    }

    /**
     * Registers a UI driver for matching client screens.
     */
    public void registerUiDriver(UiDriver driver) {
        api().registerUiDriver(driver);
    }

    /**
     * Registers an input controller for client-side interaction flows.
     */
    public void registerInputController(InputController controller) {
        api().registerInputController(controller);
    }

    /**
     * Registers a resolver for driver interaction defaults.
     */
    public void registerUiInteractionStateResolver(UiInteractionStateResolver resolver) {
        api().registerUiInteractionStateResolver(resolver);
    }

    /**
     * Registers an offscreen capture provider for client UI capture flows.
     */
    public void registerUiOffscreenCaptureProvider(UiOffscreenCaptureProvider provider) {
        api().registerUiOffscreenCaptureProvider(provider);
    }

    /**
     * Registers a framebuffer capture provider for client UI capture flows.
     */
    public void registerUiFramebufferCaptureProvider(UiFramebufferCaptureProvider provider) {
        api().registerUiFramebufferCaptureProvider(provider);
    }
}
