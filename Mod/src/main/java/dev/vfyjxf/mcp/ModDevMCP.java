package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.runtime.event.RuntimeEventPublisher;
import dev.vfyjxf.mcp.runtime.input.MinecraftInputController;
import dev.vfyjxf.mcp.runtime.inventory.VanillaInventoryDriver;
import dev.vfyjxf.mcp.runtime.hotswap.HotswapService;
import dev.vfyjxf.mcp.runtime.tool.EventToolProvider;
import dev.vfyjxf.mcp.runtime.tool.HotswapToolProvider;
import dev.vfyjxf.mcp.runtime.tool.InputToolProvider;
import dev.vfyjxf.mcp.runtime.tool.InventoryToolProvider;
import dev.vfyjxf.mcp.runtime.tool.UiToolProvider;
import dev.vfyjxf.mcp.runtime.ui.BuiltinUiCaptureProviders;
import dev.vfyjxf.mcp.runtime.ui.BuiltinUiInteractionResolvers;
import dev.vfyjxf.mcp.runtime.ui.FallbackRegionUiDriver;
import dev.vfyjxf.mcp.runtime.ui.VanillaContainerUiDriver;
import dev.vfyjxf.mcp.runtime.ui.VanillaScreenUiDriver;
import dev.vfyjxf.mcp.server.ModDevMcpServer;

import java.nio.file.Path;

public class ModDevMCP {

    public static final String modId = "mod_dev_mcp";

    private final ModDevMcpServer server;
    private final RuntimeRegistries registries;
    private final ModMcpApi api;

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
        registerBuiltinRuntime();
    }

    public void registerBuiltinProviders() {
        server.registerProvider(new UiToolProvider(registries));
        server.registerProvider(new InputToolProvider(registries));
        server.registerProvider(new InventoryToolProvider(registries));
        server.registerProvider(new EventToolProvider(registries));

        Path projectRoot = Path.of(System.getProperty("moddevmcp.project.root", System.getProperty("user.dir")));
        HotswapService hotswapService = new HotswapService(projectRoot);
        hotswapService.snapshotTimestamps();
        server.registerProvider(new HotswapToolProvider(hotswapService));
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

    private void registerBuiltinRuntime() {
        BuiltinUiInteractionResolvers.register(api);
        BuiltinUiCaptureProviders.register(api);
        api.registerUiDriver(new VanillaContainerUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        api.registerUiDriver(new VanillaScreenUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        api.registerUiDriver(new FallbackRegionUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        api.registerInventoryDriver(new VanillaInventoryDriver());
        api.registerInputController(new MinecraftInputController());
        server.registerResourceProvider(uri -> registries.uiCaptureArtifactStore().readResource(uri));
        registries.eventPublisher().publish(new dev.vfyjxf.mcp.api.event.EventEnvelope("runtime", "bootstrap", System.currentTimeMillis(), java.util.Map.of()));
    }

    public RuntimeEventPublisher eventPublisher() {
        return registries.eventPublisher();
    }
}
