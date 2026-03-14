package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.runtime.host.HostGameClient;
import dev.vfyjxf.mcp.runtime.host.HostReconnectLoop;
import dev.vfyjxf.mcp.runtime.host.HostRuntimeClientConfig;
import dev.vfyjxf.mcp.runtime.ui.ClientDevUiCaptureVerifier;
import dev.vfyjxf.mcp.runtime.ui.ClientAutomationPauseGuard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = ModDevMCP.modId, dist = Dist.CLIENT)
public class ClientEntrypoint extends ModDevMCP {

    private final HostReconnectLoop reconnectLoop;
    private final ClientRuntimeBootstrap clientBootstrap;

    public ClientEntrypoint() {
        this.clientBootstrap = new ClientRuntimeBootstrap(this);
        var config = HostRuntimeClientConfig.loadResolved();
        this.reconnectLoop = new HostReconnectLoop(
                () -> new HostGameClient(clientBootstrap.prepareClientServer(), config, "client-runtime", "client").runUntilDisconnected(),
                config.reconnectDelayMs()
        );
        this.reconnectLoop.start();
        Runtime.getRuntime().addShutdownHook(new Thread(reconnectLoop::close, "moddevmcp-client-bootstrap-shutdown"));
        new ClientAutomationPauseGuard().attach();
        new ClientDevUiCaptureVerifier(this).attach();
    }
}

