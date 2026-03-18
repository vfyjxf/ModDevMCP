package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.runtime.ui.ClientAutomationPauseGuard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = ModDevMCP.modId, dist = Dist.CLIENT)
public class ClientEntrypoint extends ModDevMCP {

    private final ClientRuntimeBootstrap clientBootstrap;
    private final IntegratedServerRuntimeHost integratedServerRuntimeHost;

    public ClientEntrypoint() {
        this.clientBootstrap = new ClientRuntimeBootstrap(this);
        this.integratedServerRuntimeHost = new IntegratedServerRuntimeHost(
                () -> {
                    new ServerRuntimeBootstrap(this).prepareServer();
                    activateServerSide();
                },
                this::deactivateServerSide
        );
        clientBootstrap.prepareClientRuntime();
        clientBootstrap.registerClientProviders();
        activateClientSide();
        startHttpService("client");
        this.integratedServerRuntimeHost.attach();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownClient, "moddevmcp-client-shutdown"));
        new ClientAutomationPauseGuard().attach();
    }

    private void shutdownClient() {
        integratedServerRuntimeHost.close();
        deactivateClientSide();
        stopHttpService();
    }
}
