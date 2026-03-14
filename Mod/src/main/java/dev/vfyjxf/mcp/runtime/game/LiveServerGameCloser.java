package dev.vfyjxf.mcp.runtime.game;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class LiveServerGameCloser implements GameCloser {

    @Override
    public boolean requestClose() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        server.execute(() -> server.halt(false));
        return true;
    }
}
