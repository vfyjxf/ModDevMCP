package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.runtime.host.HostGameClient;
import dev.vfyjxf.mcp.runtime.host.HostReconnectLoop;
import dev.vfyjxf.mcp.runtime.host.HostRuntimeClientConfig;
import dev.vfyjxf.mcp.runtime.ui.ClientAutomationPauseGuard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = ModDevMCP.modId, dist = Dist.CLIENT)
public class ClientEntrypoint extends ModDevMCP {

    private final HostReconnectLoop reconnectLoop;
    private final ClientRuntimeBootstrap clientBootstrap;
    private final IntegratedServerRuntimeHost integratedServerRuntimeHost;

    public ClientEntrypoint() {
        this.clientBootstrap = new ClientRuntimeBootstrap(this);
        var config = HostRuntimeClientConfig.loadResolved();
        this.integratedServerRuntimeHost = new IntegratedServerRuntimeHost(config);
        this.reconnectLoop = new HostReconnectLoop(
                () -> new HostGameClient(clientBootstrap.prepareClientServer(), config, "client-runtime", "client").runUntilDisconnected(),
                config.reconnectDelayMs()
        );
        this.reconnectLoop.start();
        this.integratedServerRuntimeHost.attach();
        Runtime.getRuntime().addShutdownHook(new Thread(reconnectLoop::close, "moddevmcp-client-bootstrap-shutdown"));
        Runtime.getRuntime().addShutdownHook(new Thread(integratedServerRuntimeHost::close, "moddevmcp-integrated-server-bootstrap-shutdown"));
        new ClientAutomationPauseGuard().attach();
    }
}
