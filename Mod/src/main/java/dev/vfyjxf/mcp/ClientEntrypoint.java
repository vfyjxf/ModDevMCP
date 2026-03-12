package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.runtime.ui.ClientDevUiCaptureVerifier;
import dev.vfyjxf.mcp.runtime.ui.ClientAutomationPauseGuard;
import dev.vfyjxf.mcp.server.bootstrap.EmbeddedGameMcpRuntime;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = ModDevMCP.modId, dist = Dist.CLIENT)
public class ClientEntrypoint extends ModDevMCP {

    private final EmbeddedGameMcpRuntime runtime;

    public ClientEntrypoint() {
        try {
            this.runtime = EmbeddedGameMcpRuntime.start(prepareServer());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to start game MCP runtime", exception);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                runtime.close();
            } catch (java.io.IOException ignored) {
            }
        }, "moddevmcp-client-bootstrap-shutdown"));
        new ClientAutomationPauseGuard().attach();
        new ClientDevUiCaptureVerifier(this).attach();
    }
}
