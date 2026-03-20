package dev.vfyjxf.moddev.api;

import dev.vfyjxf.moddev.api.event.EventPublisher;
import dev.vfyjxf.moddev.api.operation.OperationExecutor;
import dev.vfyjxf.moddev.api.operation.OperationRegistration;
import dev.vfyjxf.moddev.api.runtime.*;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public registration surface exposed to ModDevMCP extensions during runtime bootstrap.
 */
public final class ModDevApi {

    private final RuntimeRegistries registries;
    private final Consumer<OperationRegistration> operationRegistrationSink;

    public ModDevApi(RuntimeRegistries registries) {
        this(
                registries,
                registration -> registries.operationRegistrations().add(registration)
        );
    }

    public ModDevApi(
            RuntimeRegistries registries,
            Consumer<OperationRegistration> operationRegistrationSink
    ) {
        this.registries = Objects.requireNonNull(registries, "registries");
        this.operationRegistrationSink = Objects.requireNonNull(operationRegistrationSink, "operationRegistrationSink");
    }

    /**
     * Registers a UI driver that can inspect or automate matching screens.
     */
    public void registerUiDriver(UiDriver driver) {
        registries.uiDrivers().register(driver);
    }

    /**
     * Registers an input controller used by input and UI operations.
     */
    public void registerInputController(InputController controller) {
        registries.inputControllers().add(controller);
    }

    /**
     * Registers an operation definition and executor that can be exposed through the HTTP operation API.
     */
    public void registerOperation(OperationDefinition definition, OperationExecutor executor) {
        operationRegistrationSink.accept(new OperationRegistration(definition, executor));
    }

    /**
     * Registers a resolver that can derive interaction defaults for matching UI contexts.
     */
    public void registerUiInteractionStateResolver(UiInteractionStateResolver resolver) {
        registries.uiInteractionResolvers().register(resolver);
    }

    /**
     * Registers an offscreen capture provider for matching UI contexts.
     */
    public void registerUiOffscreenCaptureProvider(UiOffscreenCaptureProvider provider) {
        registries.uiOffscreenCaptureProviders().register(provider);
    }

    /**
     * Registers a framebuffer capture provider for matching UI contexts.
     */
    public void registerUiFramebufferCaptureProvider(UiFramebufferCaptureProvider provider) {
        registries.uiFramebufferCaptureProviders().register(provider);
    }

    /**
     * Returns the shared event publisher associated with the active runtime registries.
     */
    public EventPublisher eventPublisher() {
        return registries.eventPublisher();
    }
}
