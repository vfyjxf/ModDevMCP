package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.ui.UiTarget;

final class PauseScreenBackToGameSupport {

    private static final String PAUSE_SCREEN_CLASS = "net.minecraft.client.gui.screens.PauseScreen";
    private static final String BACK_TO_GAME_TARGET_ID = "button-back-to-game";

    private PauseScreenBackToGameSupport() {
    }

    static boolean resumeIfPauseBackTarget(
            String screenClassName,
            UiTarget target,
            Runnable closeScreen,
            Runnable grabMouse
    ) {
        if (!PAUSE_SCREEN_CLASS.equals(screenClassName) || target == null || !BACK_TO_GAME_TARGET_ID.equals(target.targetId())) {
            return false;
        }
        closeScreen.run();
        grabMouse.run();
        return true;
    }
}

