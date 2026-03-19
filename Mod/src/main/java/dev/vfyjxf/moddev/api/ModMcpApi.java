package dev.vfyjxf.moddev.api;

import dev.vfyjxf.moddev.api.event.EventPublisher;
import dev.vfyjxf.moddev.api.operation.OperationExecutor;
import dev.vfyjxf.moddev.api.operation.OperationRegistration;
import dev.vfyjxf.moddev.api.runtime.*;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;
import dev.vfyjxf.moddev.server.api.McpToolProvider;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public registration surface exposed to ModDevMCP extensions during runtime bootstrap.
 *
 * <p>Downstream mods can use this API to contribute drivers, runtime adapters, tool providers,
 * and operation bindings without depending on internal bootstrap classes.
 */
public final class ModMcpApi {

    private final RuntimeRegistries registries;
    private final Consumer<McpToolProvider> toolProviderSink;
    private final Consumer<OperationRegistration> operationRegistrationSink;

    public ModMcpApi(RuntimeRegistries registries) {
        this(
                registries,
                provider -> registries.toolProviders().add(provider),
                registration -> registries.operationRegistrations().add(registration)
        );
    }

    public ModMcpApi(RuntimeRegistries registries, Consumer<McpToolProvider> toolProviderSink) {
        this(
                registries,
                toolProviderSink,
                registration -> registries.operationRegistrations().add(registration)
        );
    }

    public ModMcpApi(
            RuntimeRegistries registries,
            Consumer<McpToolProvider> toolProviderSink,
            Consumer<OperationRegistration> operationRegistrationSink
    ) {
        this.registries = registries;
        this.toolProviderSink = Objects.requireNonNull(toolProviderSink, "toolProviderSink");
        this.operationRegistrationSink = Objects.requireNonNull(operationRegistrationSink, "operationRegistrationSink");
    }

    /**
     * Registers a UI driver that can inspect or automate matching screens.
     */
    public void registerUiDriver(UiDriver driver) {
        registries.uiDrivers().register(driver);
    }

    /**
     * Registers an input controller used by UI and interaction tools.
     */
    public void registerInputController(InputController controller) {
        registries.inputControllers().add(controller);
    }

    /**
     * Registers an MCP tool provider to be exposed through the active ModDevMCP server.
     */
    public void registerToolProvider(McpToolProvider provider) {
        toolProviderSink.accept(provider);
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