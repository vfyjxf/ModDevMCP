package dev.vfyjxf.mcp.runtime.ui;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Objects;

public final class ClientAutomationPauseGuard {

    private static final String PAUSE_SCREEN_CLASS = "net.minecraft.client.gui.screens.PauseScreen";

    public void attach() {
        NeoForge.EVENT_BUS.addListener(this::onClientTickPost);
    }

    private void onClientTickPost(ClientTickEvent.Post event) {
        apply(new MinecraftGuardClient(Minecraft.getInstance()));
    }

    static void apply(GuardClient client) {
        var shouldForceWindowActive = client.hasLoadedLevel() && !client.isWindowActive();
        if (!shouldForceWindowActive) {
            return;
        }
        client.setWindowActive(true);
        if (!Objects.equals(PAUSE_SCREEN_CLASS, client.screenClassName())) {
            return;
        }
        client.clearPause();
        client.closeScreen();
        client.grabMouse();
    }

    interface GuardClient {

        boolean hasLoadedLevel();

        boolean isWindowActive();

        String screenClassName();

        void setWindowActive(boolean active);

        void clearPause();

        void closeScreen();

        void grabMouse();
    }

    private record MinecraftGuardClient(Minecraft minecraft) implements GuardClient {

        @Override
        public boolean hasLoadedLevel() {
            return minecraft.level != null;
        }

        @Override
        public boolean isWindowActive() {
            return minecraft.isWindowActive();
        }

        @Override
        public String screenClassName() {
            return minecraft.screen == null ? null : minecraft.screen.getClass().getName();
        }

        @Override
        public void setWindowActive(boolean active) {
            minecraft.setWindowActive(active);
        }

        @Override
        public void clearPause() {
            minecraft.pauseGame(false);
        }

        @Override
        public void closeScreen() {
            minecraft.setScreen(null);
        }

        @Override
        public void grabMouse() {
            minecraft.mouseHandler.grabMouse();
        }
    }
}
