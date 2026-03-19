package dev.vfyjxf.moddev;

import dev.vfyjxf.moddev.runtime.command.LiveServerCommandService;
import dev.vfyjxf.moddev.runtime.game.LiveServerGameCloser;
import dev.vfyjxf.moddev.runtime.tool.CommandToolProvider;
import dev.vfyjxf.moddev.runtime.tool.GameToolProvider;
import dev.vfyjxf.moddev.server.ModDevMcpServer;

public final class ServerRuntimeBootstrap {

    private final ModDevMCP mod;

    public ServerRuntimeBootstrap(ModDevMCP mod) {
        this.mod = mod;
    }

    public ModDevMcpServer prepareServer() {
        mod.prepareCommonServer();
        prepareServerRuntime();
        registerServerProviders();
        return mod.server();
    }

    public void prepareServerRuntime() {
        mod.prepareCommonServer();
        mod.claimServerRuntimeRegistration();
    }

    public void registerServerProviders() {
        if (!mod.claimServerProviderRegistration()) {
            return;
        }
        mod.registerToolProvider(GameToolProvider.serverOnly(new LiveServerGameCloser()));
        mod.registerToolProvider(CommandToolProvider.serverOnly(new LiveServerCommandService()));
        mod.registerServerRegistrarProviders();
    }
}

