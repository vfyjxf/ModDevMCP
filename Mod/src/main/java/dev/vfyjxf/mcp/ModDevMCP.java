package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.runtime.event.RuntimeEventPublisher;
import dev.vfyjxf.mcp.runtime.input.MinecraftInputController;
import dev.vfyjxf.mcp.runtime.inventory.VanillaInventoryDriver;
import dev.vfyjxf.mcp.runtime.tool.EventToolProvider;
import dev.vfyjxf.mcp.runtime.tool.InputToolProvider;
import dev.vfyjxf.mcp.runtime.tool.InventoryToolProvider;
import dev.vfyjxf.mcp.runtime.tool.UiToolProvider;
import dev.vfyjxf.mcp.runtime.ui.BuiltinUiCaptureProviders;
import dev.vfyjxf.mcp.runtime.ui.BuiltinUiInteractionResolvers;
import dev.vfyjxf.mcp.runtime.ui.FallbackRegionUiDriver;
import dev.vfyjxf.mcp.runtime.ui.LiveClientScreenProbe;
import dev.vfyjxf.mcp.runtime.ui.VanillaContainerUiDriver;
import dev.vfyjxf.mcp.runtime.ui.VanillaScreenUiDriver;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ModDevMCP {

    public static final String modId = "mod_dev_mcp";

    private final ModDevMcpServer server;
    private final RuntimeRegistries registries;
    private final ModMcpApi api;
    private final McpToolProvider uiToolProvider;
    private final McpToolProvider inputToolProvider;
    private final McpToolProvider inventoryToolProvider;
    private final McpToolProvider eventToolProvider;
    private final Set<McpToolProvider> registeredToolProviders = Collections.newSetFromMap(new IdentityHashMap<>());

    public ModDevMCP() {
        this(new ModDevMcpServer(), new RuntimeRegistries());
    }

    public ModDevMCP(ModDevMcpServer server) {
        this(server, new RuntimeRegistries());
    }

    public ModDevMCP(ModDevMcpServer server, RuntimeRegistries registries) {
        this.server = server;
        this.registries = registries;
        this.api = new ModMcpApi(registries);
        this.uiToolProvider = new UiToolProvider(registries, new LiveClientScreenProbe());
        this.inputToolProvider = new InputToolProvider(registries);
        this.inventoryToolProvider = new InventoryToolProvider(registries);
        this.eventToolProvider = new EventToolProvider(registries);
        registerBuiltinRuntime();
    }

    public synchronized void registerBuiltinProviders() {
        registerToolProvider(uiToolProvider);
        registerToolProvider(inputToolProvider);
        registerToolProvider(inventoryToolProvider);
        registerToolProvider(eventToolProvider);
        registries.toolProviders().forEach(this::registerToolProvider);
    }

    public ModDevMcpServer server() {
        return server;
    }

    public RuntimeRegistries registries() {
        return registries;
    }

    public ModMcpApi api() {
        return api;
    }

    public synchronized ModDevMcpServer prepareServer() {
        registerBuiltinProviders();
        return server;
    }

    private void registerBuiltinRuntime() {
        BuiltinUiInteractionResolvers.register(api);
        BuiltinUiCaptureProviders.register(api);
        api.registerUiDriver(new VanillaContainerUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        api.registerUiDriver(new VanillaScreenUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        api.registerUiDriver(new FallbackRegionUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        api.registerInventoryDriver(new VanillaInventoryDriver());
        api.registerInputController(new MinecraftInputController(registries.uiPointerStates()));
        server.registerResourceProvider(uri -> registries.uiCaptureArtifactStore().readResource(uri));
        registries.eventPublisher().publish(new dev.vfyjxf.mcp.api.event.EventEnvelope("runtime", "bootstrap", System.currentTimeMillis(), java.util.Map.of()));
    }

    public RuntimeEventPublisher eventPublisher() {
        return registries.eventPublisher();
    }

    private void registerToolProvider(McpToolProvider provider) {
        if (registeredToolProviders.add(provider)) {
            server.registerProvider(provider);
        }
    }
}
