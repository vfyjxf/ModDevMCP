package dev.vfyjxf.mcp.api;

import dev.vfyjxf.mcp.api.event.EventPublisher;
import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public registration surface exposed to ModDevMCP extensions during runtime bootstrap.
 *
 * <p>Downstream mods can use this API to contribute drivers, runtime adapters, and tool providers
 * without depending on internal bootstrap classes.
 */
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
