package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.ModDevMCP;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientDevUiCaptureVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDevUiCaptureVerifier.class);

    private final DevUiCaptureVerificationRunner runner;
    private boolean disabled;

    public ClientDevUiCaptureVerifier(ModDevMCP mod) {
        this.runner = new DevUiCaptureVerificationRunner(
                mod,
                FMLLoader.getGamePath().resolve("build").resolve("moddevmcp").resolve("dev-verification")
        );
    }

    public void attach() {
        if (DevUiCaptureFlags.shouldAttach(FMLLoader.isProduction())) {
            NeoForge.EVENT_BUS.addListener(this::onScreenRenderPost);
        }
    }

    private void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (disabled || runner.completed()) {
            return;
        }
        try {
            if (runner.captureOnce(
                    event.getScreen().getClass().getName(),
                    "minecraft",
                    event.getMouseX(),
                    event.getMouseY()
            )) {
                LOGGER.info("Wrote dev UI capture verification report to {}", runner.lastReportPath().orElse(null));
            }
        } catch (RuntimeException exception) {
            disabled = true;
            LOGGER.error("Failed to run dev UI capture verification", exception);
        }
    }
}
