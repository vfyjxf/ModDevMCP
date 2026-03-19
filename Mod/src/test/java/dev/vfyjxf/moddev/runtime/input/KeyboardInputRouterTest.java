package dev.vfyjxf.moddev.runtime.input;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardInputRouterTest {

    private static final int KEY_A = 65;
    private static final int KEY_B = 66;
    private static final int KEY_LEFT_CONTROL = 341;
    private static final int KEY_LEFT_SHIFT = 340;
    private static final int MOD_SHIFT = 1;
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
        assertEquals(0, screen.keyPressedCalls);
        assertEquals(0, screen.keyReleasedCalls);
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

    @Test
    void keyPressWithModifiersUsesCanonicalDispatcherEvenWhenScreenWouldConsumePress() {
        var screen = new RecordingScreenInput(true, false, false);
        var fallback = new RecordingFallbackInput();

        var result = KeyboardInputRouter.keyPress(
                new InputCommand("key_press", 0.0d, 0.0d, 0, KEY_A, 0, MOD_CONTROL, null, 0),
                screen,
                fallback
        );

        assertTrue(result.accepted());
        assertEquals(0, screen.keyPressedCalls);
        assertEquals(0, screen.keyReleasedCalls);
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

    @Test
    void keyDownDispatchesOnlyKeyDownEvent() {
        var fallback = new RecordingFallbackInput();

        var result = KeyboardInputRouter.keyDown(
                new InputCommand("key_down", 0.0d, 0.0d, 0, KEY_A, 0, 0, null, 0),
                fallback
        );

        assertTrue(result.accepted());
        assertEquals(List.of("down:" + KEY_A), fallback.events);
    }

    @Test
    void keyUpDispatchesOnlyKeyUpEvent() {
        var fallback = new RecordingFallbackInput();

        var result = KeyboardInputRouter.keyUp(
                new InputCommand("key_up", 0.0d, 0.0d, 0, KEY_A, 0, 0, null, 0),
                fallback
        );

        assertTrue(result.accepted());
        assertEquals(List.of("up:" + KEY_A), fallback.events);
    }

    @Test
    void explicitModifierHoldAppliesToLaterPlainKeyEvents() {
        var state = new VirtualModifierState();
        var fallback = new RecordingFallbackInput();

        state.keyDown(KEY_LEFT_SHIFT);
        var result = KeyboardInputRouter.keyDown(command(KEY_A, 0), fallback, state);

        assertTrue(result.accepted());
        assertEquals(List.of("down:65:1"), fallback.eventsWithModifiers);
    }

    @Test
    void oneShotClickModifiersDoNotPersistAfterDispatch() {
        var state = new VirtualModifierState();
        var fallback = new RecordingFallbackInput();

        KeyboardInputRouter.keyClick(
                new InputCommand("key_click", 0.0d, 0.0d, 0, KEY_A, 0, MOD_SHIFT, null, 0),
                null,
                fallback,
                state
        );
        KeyboardInputRouter.keyDown(command(KEY_B, 0), fallback, state);

        assertEquals(
                List.of("down:340:1", "down:65:1", "up:65:1", "up:340:1", "down:66:0"),
                fallback.eventsWithModifiers
        );
    }

    private static InputCommand command(int keyCode, int modifiers) {
        return new InputCommand("key_down", 0.0d, 0.0d, 0, keyCode, 0, modifiers, null, 0);
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
        private final List<String> eventsWithModifiers = new ArrayList<>();

        @Override
        public void dispatchKeyDown(int keyCode, int scanCode, int modifiers) {
            events.add("down:" + keyCode);
            eventsWithModifiers.add("down:" + keyCode + ":" + modifiers);
        }

        @Override
        public void dispatchKeyUp(int keyCode, int scanCode, int modifiers) {
            events.add("up:" + keyCode);
            eventsWithModifiers.add("up:" + keyCode + ":" + modifiers);
        }
    }
}

