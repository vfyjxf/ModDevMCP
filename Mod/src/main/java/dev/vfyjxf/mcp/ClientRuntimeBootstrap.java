package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.runtime.game.LiveClientGameCloser;
import dev.vfyjxf.mcp.runtime.command.LiveClientCommandService;
import dev.vfyjxf.mcp.runtime.input.MinecraftInputController;
import dev.vfyjxf.mcp.runtime.game.LiveClientPauseOnLostFocusService;
import dev.vfyjxf.mcp.runtime.world.LiveClientWorldService;
import dev.vfyjxf.mcp.runtime.tool.CommandToolProvider;
import dev.vfyjxf.mcp.runtime.tool.GameToolProvider;
import dev.vfyjxf.mcp.runtime.tool.InputToolProvider;
import dev.vfyjxf.mcp.runtime.tool.PauseOnLostFocusToolProvider;
import dev.vfyjxf.mcp.runtime.tool.UiToolProvider;
import dev.vfyjxf.mcp.runtime.tool.WorldToolProvider;
import dev.vfyjxf.mcp.runtime.ui.*;
import dev.vfyjxf.mcp.server.ModDevMcpServer;

public final class ClientRuntimeBootstrap {

    private final ModDevMCP mod;

    public ClientRuntimeBootstrap(ModDevMCP mod) {
        this.mod = mod;
    }

    public ModDevMcpServer prepareClientServer() {
        mod.prepareCommonServer();
        prepareClientRuntime();
        registerClientProviders();
        return mod.server();
    }

    public void prepareClientRuntime() {
        mod.prepareCommonServer();
        if (!mod.claimClientRuntimeRegistration()) {
            return;
        }
        var registries = mod.registries();
        var api = mod.api();
        BuiltinUiInteractionResolvers.register(api);
        BuiltinUiCaptureProviders.register(api);
        api.registerUiDriver(new VanillaContainerUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        api.registerUiDriver(new VanillaScreenUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        api.registerUiDriver(new FallbackRegionUiDriver(registries.uiSessionStates(), registries.uiInteractionResolvers()));
        api.registerInputController(new MinecraftInputController(registries.uiPointerStates()));
    }

    public void registerClientProviders() {
        if (!mod.claimClientProviderRegistration()) {
            return;
        }
        var registries = mod.registries();
        mod.registerToolProvider(new UiToolProvider(registries, new LiveClientScreenProbe()));
        mod.registerToolProvider(new InputToolProvider(registries));
        mod.registerToolProvider(new GameToolProvider(new LiveClientGameCloser()));
        mod.registerToolProvider(new PauseOnLostFocusToolProvider(new LiveClientPauseOnLostFocusService()));
        mod.registerToolProvider(CommandToolProvider.clientOnly(new LiveClientCommandService()));
        mod.registerToolProvider(new WorldToolProvider(new LiveClientWorldService()));
        mod.registerClientRegistrarProviders();
    }
}
