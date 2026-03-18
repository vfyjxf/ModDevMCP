package dev.vfyjxf.mcp;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = ModDevMCP.modId, dist = Dist.DEDICATED_SERVER)
public class ServerEntrypoint extends ModDevMCP {

    public ServerEntrypoint() {
        var serverBootstrap = new ServerRuntimeBootstrap(this);
        serverBootstrap.prepareServerRuntime();
        serverBootstrap.registerServerProviders();
        activateServerSide();
        startHttpService("server");
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownServer, "moddevmcp-server-shutdown"));
    }

    private void shutdownServer() {
        deactivateServerSide();
        stopHttpService();
    }
}
