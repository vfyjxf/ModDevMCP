package dev.vfyjxf.mcp.runtime.input;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardInputRouterTest {

    private static final int KEY_A = 65;
    private static final int KEY_LEFT_CONTROL = 341;
    private static final int MOD_CONTROL = 2;

    @Test
    void keyPressFallsBackToKeyboardDispatcherWhenScreenDoesNotHandleIt() {
        var screen = new RecordingScreenInput(false, false, false);
        var fallback = new RecordingFallbackInput();

        var result = KeyboardInputRouter.keyPress(
                new InputCommand("key_press", 0.0d, 0.0d, 0, 69, 0, 0, null, 0),
                screen,
                fallback
        );

        assertTrue(result.accepted());
        assertEquals(1, screen.keyPressedCalls);
        assertEquals(1, screen.keyReleasedCalls);
        assertEquals(List.of("down:69", "up:69"), fallback.events);
    }

    @Test
    void keyPressDispatchesModifierKeySequenceBeforeMainKey() {
        var screen = new RecordingScreenInput(false, false, false);
        var fallback = new RecordingFallbackInput();

        var result = KeyboardInputRouter.keyPress(
                new InputCommand("key_press", 0.0d, 0.0d, 0, KEY_A, 0, MOD_CONTROL, null, 0),
                screen,
                fallback
        );

        assertTrue(result.accepted());
        assertEquals(
                List.of(
                        "down:" + KEY_LEFT_CONTROL,
                        "down:" + KEY_A,
                        "up:" + KEY_A,
                        "up:" + KEY_LEFT_CONTROL
                ),
                fallback.events
        );
    }

    private static final class RecordingScreenInput implements KeyboardInputRouter.ScreenInput {

        private final boolean keyPressedResult;
        private final boolean keyReleasedResult;
        private final boolean charTypedResult;
        private int keyPressedCalls;
        private int keyReleasedCalls;
        private int charTypedCalls;

        private RecordingScreenInput(boolean keyPressedResult, boolean keyReleasedResult, boolean charTypedResult) {
            this.keyPressedResult = keyPressedResult;
            this.keyReleasedResult = keyReleasedResult;
            this.charTypedResult = charTypedResult;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            keyPressedCalls++;
            return keyPressedResult;
        }

        @Override
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            keyReleasedCalls++;
            return keyReleasedResult;
        }

        @Override
        public boolean charTyped(char character, int modifiers) {
            charTypedCalls++;
            return charTypedResult;
        }
    }

    private static final class RecordingFallbackInput implements KeyboardInputRouter.FallbackInput {

        private final List<String> events = new ArrayList<>();

        @Override
        public void dispatchKeyDown(int keyCode, int scanCode, int modifiers) {
            events.add("down:" + keyCode);
        }

        @Override
        public void dispatchKeyUp(int keyCode, int scanCode, int modifiers) {
            events.add("up:" + keyCode);
        }
    }
}
