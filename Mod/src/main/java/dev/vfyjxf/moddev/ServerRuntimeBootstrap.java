package dev.vfyjxf.moddev;

import dev.vfyjxf.moddev.runtime.command.LiveServerCommandService;
import dev.vfyjxf.moddev.runtime.game.LiveServerGameCloser;

public final class ServerRuntimeBootstrap {

    private final ModDevMCP mod;

    public ServerRuntimeBootstrap(ModDevMCP mod) {
        this.mod = mod;
    }

    public ModDevMCP prepareServer() {
        mod.prepareCommonServer();
        prepareServerRuntime();
        registerServerProviders();
        return mod;
    }

    public void prepareServerRuntime() {
        mod.prepareCommonServer();
        mod.claimServerRuntimeRegistration();
    }

    public void registerServerProviders() {
        if (!mod.claimServerProviderRegistration()) {
            return;
        }
        var registries = mod.registries();
        registries.registerGameCloser("server", new LiveServerGameCloser());
        registries.registerCommandService("server", new LiveServerCommandService());
        mod.registerServerRegistrarProviders();
    }
}
