package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.ui.Bounds;
import dev.vfyjxf.mcp.api.ui.UiTarget;
import dev.vfyjxf.mcp.api.ui.UiTargetState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PauseScreenBackToGameSupportTest {

    @Test
    void resumeIfPauseBackTargetClosesScreenAndGrabsMouse() {
        var closed = new AtomicBoolean(false);
        var grabbed = new AtomicBoolean(false);

        var resumed = PauseScreenBackToGameSupport.resumeIfPauseBackTarget(
                "net.minecraft.client.gui.screens.PauseScreen",
                target("button-back-to-game", "Back to Game"),
                () -> closed.set(true),
                () -> grabbed.set(true)
        );

        assertTrue(resumed);
        assertTrue(closed.get());
        assertTrue(grabbed.get());
    }

    @Test
    void resumeIfPauseBackTargetReturnsFalseForOtherTargets() {
        var closed = new AtomicBoolean(false);
        var grabbed = new AtomicBoolean(false);

        var resumed = PauseScreenBackToGameSupport.resumeIfPauseBackTarget(
                "net.minecraft.client.gui.screens.PauseScreen",
                target("button-options", "Options..."),
                () -> closed.set(true),
                () -> grabbed.set(true)
        );

        assertFalse(resumed);
        assertFalse(closed.get());
        assertFalse(grabbed.get());
    }

    private UiTarget target(String targetId, String text) {
        return new UiTarget(
                targetId,
                "vanilla-screen",
                "net.minecraft.client.gui.screens.PauseScreen",
                "minecraft",
                "button",
                text,
                new Bounds(0, 0, 10, 10),
                UiTargetState.defaultState(),
                List.of("click"),
                Map.of()
        );
    }
}
