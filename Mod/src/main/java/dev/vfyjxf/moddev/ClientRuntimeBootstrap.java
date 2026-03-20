package dev.vfyjxf.moddev;

import dev.vfyjxf.moddev.runtime.command.LiveClientCommandService;
import dev.vfyjxf.moddev.runtime.game.LiveClientGameCloser;
import dev.vfyjxf.moddev.runtime.game.LiveClientPauseOnLostFocusService;
import dev.vfyjxf.moddev.runtime.input.MinecraftInputController;
import dev.vfyjxf.moddev.runtime.ui.BuiltinUiCaptureProviders;
import dev.vfyjxf.moddev.runtime.ui.BuiltinUiInteractionResolvers;
import dev.vfyjxf.moddev.runtime.ui.FallbackRegionUiDriver;
import dev.vfyjxf.moddev.runtime.ui.LiveClientScreenProbe;
import dev.vfyjxf.moddev.runtime.ui.VanillaContainerUiDriver;
import dev.vfyjxf.moddev.runtime.ui.VanillaScreenUiDriver;
import dev.vfyjxf.moddev.runtime.world.LiveClientWorldService;

public final class ClientRuntimeBootstrap {

    private final ModDevMCP mod;

    public ClientRuntimeBootstrap(ModDevMCP mod) {
        this.mod = mod;
    }

    public ModDevMCP prepareClientServer() {
        mod.prepareCommonServer();
        prepareClientRuntime();
        registerClientProviders();
        return mod;
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
        registries.registerScreenProbe("client", new LiveClientScreenProbe());
        registries.registerGameCloser("client", new LiveClientGameCloser());
        registries.registerPauseOnLostFocusService("client", new LiveClientPauseOnLostFocusService());
        registries.registerCommandService("client", new LiveClientCommandService());
        registries.registerWorldService("client", new LiveClientWorldService());
        mod.registerClientRegistrarProviders();
    }
}
