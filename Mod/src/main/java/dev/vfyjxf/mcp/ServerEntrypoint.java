package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.runtime.host.HostGameClient;
import dev.vfyjxf.mcp.runtime.host.HostReconnectLoop;
import dev.vfyjxf.mcp.runtime.host.HostRuntimeClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = ModDevMCP.modId, dist = Dist.DEDICATED_SERVER)
public class ServerEntrypoint extends ModDevMCP {

    private final HostReconnectLoop reconnectLoop;
    private final ServerRuntimeBootstrap serverBootstrap;

    public ServerEntrypoint() {
        this.serverBootstrap = new ServerRuntimeBootstrap(this);
        var config = HostRuntimeClientConfig.loadResolved();
        this.reconnectLoop = new HostReconnectLoop(
                () -> new HostGameClient(serverBootstrap.prepareServer(), config, "server-runtime", "server").runUntilDisconnected(),
                config.reconnectDelayMs()
        );
        this.reconnectLoop.start();
        Runtime.getRuntime().addShutdownHook(new Thread(reconnectLoop::close, "moddevmcp-server-bootstrap-shutdown"));
    }
}
