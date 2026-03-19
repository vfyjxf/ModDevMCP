package dev.vfyjxf.moddev.runtime.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientAutomationPauseGuardTest {

    @Test
    void applyForcesWindowActiveAndDismissesPauseScreenWhenWorldLostFocus() {
        var client = new FakeGuardClient(
                true,
                false,
                "net.minecraft.client.gui.screens.PauseScreen"
        );

        ClientAutomationPauseGuard.apply(client);

        assertTrue(client.windowActivated.get());
        assertTrue(client.pauseCleared.get());
        assertTrue(client.screenClosed.get());
        assertTrue(client.mouseGrabbed.get());
    }

    @Test
    void applyOnlyForcesWindowActiveForNonPauseScreen() {
        var client = new FakeGuardClient(
                true,
                false,
                "net.minecraft.client.gui.screens.inventory.InventoryScreen"
        );

        ClientAutomationPauseGuard.apply(client);

        assertTrue(client.windowActivated.get());
        assertFalse(client.pauseCleared.get());
        assertFalse(client.screenClosed.get());
        assertFalse(client.mouseGrabbed.get());
    }

    @Test
    void applyDoesNothingWithoutWorldLoaded() {
        var client = new FakeGuardClient(
                false,
                false,
                "net.minecraft.client.gui.screens.PauseScreen"
        );

        ClientAutomationPauseGuard.apply(client);

        assertFalse(client.windowActivated.get());
        assertFalse(client.pauseCleared.get());
        assertFalse(client.screenClosed.get());
        assertFalse(client.mouseGrabbed.get());
    }

    @Test
    void applyDoesNotDismissPauseScreenWhenWindowAlreadyActive() {
        var client = new FakeGuardClient(
                true,
                true,
                "net.minecraft.client.gui.screens.PauseScreen"
        );

        ClientAutomationPauseGuard.apply(client);

        assertFalse(client.windowActivated.get());
        assertFalse(client.pauseCleared.get());
        assertFalse(client.screenClosed.get());
        assertFalse(client.mouseGrabbed.get());
    }

    private static final class FakeGuardClient implements ClientAutomationPauseGuard.GuardClient {

        private final boolean levelLoaded;
        private final boolean windowActive;
        private final String screenClassName;
        private final AtomicBoolean windowActivated = new AtomicBoolean(false);
        private final AtomicBoolean pauseCleared = new AtomicBoolean(false);
        private final AtomicBoolean screenClosed = new AtomicBoolean(false);
        private final AtomicBoolean mouseGrabbed = new AtomicBoolean(false);

        private FakeGuardClient(boolean levelLoaded, boolean windowActive, String screenClassName) {
            this.levelLoaded = levelLoaded;
            this.windowActive = windowActive;
            this.screenClassName = screenClassName;
        }

        @Override
        public boolean hasLoadedLevel() {
            return levelLoaded;
        }

        @Override
        public boolean isWindowActive() {
            return windowActive;
        }

        @Override
        public String screenClassName() {
            return screenClassName;
        }

        @Override
        public void setWindowActive(boolean active) {
            if (active) {
                windowActivated.set(true);
            }
        }

        @Override
        public void clearPause() {
            pauseCleared.set(true);
        }

        @Override
        public void closeScreen() {
            screenClosed.set(true);
        }

        @Override
        public void grabMouse() {
            mouseGrabbed.set(true);
        }
    }
}

