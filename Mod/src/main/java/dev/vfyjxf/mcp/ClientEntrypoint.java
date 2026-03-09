package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.runtime.ui.ClientDevUiCaptureVerifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = ModDevMCP.modId, dist = Dist.CLIENT)
public class ClientEntrypoint extends ModDevMCP {

    public ClientEntrypoint() {
        new ClientDevUiCaptureVerifier(this).attach();
    }
}
