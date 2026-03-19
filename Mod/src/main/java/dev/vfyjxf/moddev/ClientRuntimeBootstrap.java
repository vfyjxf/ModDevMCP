package dev.vfyjxf.moddev;

import dev.vfyjxf.moddev.runtime.command.LiveClientCommandService;
import dev.vfyjxf.moddev.runtime.game.LiveClientGameCloser;
import dev.vfyjxf.moddev.runtime.game.LiveClientPauseOnLostFocusService;
import dev.vfyjxf.moddev.runtime.input.MinecraftInputController;
import dev.vfyjxf.moddev.runtime.tool.CommandToolProvider;
import dev.vfyjxf.moddev.runtime.tool.GameToolProvider;
import dev.vfyjxf.moddev.runtime.tool.InputToolProvider;
import dev.vfyjxf.moddev.runtime.tool.PauseOnLostFocusToolProvider;
import dev.vfyjxf.moddev.runtime.tool.UiToolProvider;
import dev.vfyjxf.moddev.runtime.tool.WorldToolProvider;
import dev.vfyjxf.moddev.runtime.ui.BuiltinUiCaptureProviders;
import dev.vfyjxf.moddev.runtime.ui.BuiltinUiInteractionResolvers;
import dev.vfyjxf.moddev.runtime.ui.FallbackRegionUiDriver;
import dev.vfyjxf.moddev.runtime.ui.LiveClientScreenProbe;
import dev.vfyjxf.moddev.runtime.ui.VanillaContainerUiDriver;
import dev.vfyjxf.moddev.runtime.ui.VanillaScreenUiDriver;
import dev.vfyjxf.moddev.runtime.world.LiveClientWorldService;
import dev.vfyjxf.moddev.server.ModDevMcpServer;

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
        mod.registerToolProvider(GameToolProvider.clientOnly(new LiveClientGameCloser()));
        mod.registerToolProvider(new PauseOnLostFocusToolProvider(new LiveClientPauseOnLostFocusService()));
        mod.registerToolProvider(CommandToolProvider.clientOnly(new LiveClientCommandService()));
        mod.registerToolProvider(new WorldToolProvider(new LiveClientWorldService()));
        mod.registerClientRegistrarProviders();
    }
}

