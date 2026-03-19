package dev.vfyjxf.moddev.api.event;

import dev.vfyjxf.moddev.api.ModMcpApi;
import dev.vfyjxf.moddev.api.runtime.InputController;
import dev.vfyjxf.moddev.api.runtime.UiDriver;
import dev.vfyjxf.moddev.api.runtime.UiFramebufferCaptureProvider;
import dev.vfyjxf.moddev.api.runtime.UiInteractionStateResolver;
import dev.vfyjxf.moddev.api.runtime.UiOffscreenCaptureProvider;

/**
 * Client-side registrar event used to register operations and client runtime adapters.
 */
public final class RegisterClientOperationsEvent extends RegisterOperationsEvent {

    public RegisterClientOperationsEvent(ModMcpApi api, EventPublisher eventPublisher) {
        super(api, eventPublisher);
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