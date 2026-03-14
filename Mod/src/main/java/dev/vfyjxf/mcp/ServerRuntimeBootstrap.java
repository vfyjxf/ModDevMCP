package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.runtime.game.LiveServerGameCloser;
import dev.vfyjxf.mcp.runtime.tool.GameToolProvider;
import dev.vfyjxf.mcp.server.ModDevMcpServer;

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
        mod.registerToolProvider(new GameToolProvider(new LiveServerGameCloser()));
    }
}
