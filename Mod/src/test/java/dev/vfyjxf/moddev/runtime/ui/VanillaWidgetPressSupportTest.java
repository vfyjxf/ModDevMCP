package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.ui.Bounds;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import dev.vfyjxf.moddev.api.ui.UiTargetState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaWidgetPressSupportTest {

    @Test
    void invokeButtonPressCallsMatchingButtonOnPress() {
        var pressed = new AtomicBoolean(false);
        var screen = new FakeScreen();
        screen.addButton(new FakeButton("Back to Game", 111, 62, 204, 20, () -> pressed.set(true)));
        var target = buttonTarget("button-back-to-game", "Back to Game", 111, 62, 204, 20);

        var invoked = VanillaWidgetPressSupport.invokeButtonPress(screen, target);

        assertTrue(invoked);
        assertTrue(pressed.get());
    }

    @Test
    void invokeButtonPressReturnsFalseWhenTargetDoesNotMatchAnyButton() {
        var pressed = new AtomicBoolean(false);
        var screen = new FakeScreen();
        screen.addButton(new FakeButton("Options...", 111, 134, 98, 20, () -> pressed.set(true)));
        var target = buttonTarget("button-back-to-game", "Back to Game", 111, 62, 204, 20);

        var invoked = VanillaWidgetPressSupport.invokeButtonPress(screen, target);

        assertFalse(invoked);
        assertFalse(pressed.get());
    }

    private UiTarget buttonTarget(String targetId, String text, int x, int y, int width, int height) {
        return new UiTarget(
                targetId,
                "vanilla-screen",
                "net.minecraft.client.gui.screens.PauseScreen",
                "minecraft",
                "button",
                text,
                new Bounds(x, y, width, height),
                UiTargetState.defaultState(),
                List.of("click", "hover"),
                Map.of("widgetClass", "net.minecraft.client.gui.components.Button")
        );
    }

    private static final class FakeScreen {

        private final List<Object> children = new ArrayList<>();

        private void addButton(FakeButton button) {
            children.add(button);
        }

        @SuppressWarnings("unused")
        public List<Object> children() {
            return List.copyOf(children);
        }
    }

    private static final class FakeButton {

        private final String message;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final Runnable onPress;

        private FakeButton(String message, int x, int y, int width, int height, Runnable onPress) {
            this.message = message;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.onPress = onPress;
        }

        @SuppressWarnings("unused")
        public int getX() {
            return x;
        }

        @SuppressWarnings("unused")
        public int getY() {
            return y;
        }

        @SuppressWarnings("unused")
        public int getWidth() {
            return width;
        }

        @SuppressWarnings("unused")
        public int getHeight() {
            return height;
        }

        @SuppressWarnings("unused")
        public Object getMessage() {
            return new Object() {
                @Override
                public String toString() {
                    return message;
                }
            };
        }

        @SuppressWarnings("unused")
        public void onPress() {
            onPress.run();
        }
    }
}

