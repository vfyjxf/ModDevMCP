package dev.vfyjxf.mcp.runtime.input;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ModifiedKeybindingDispatchTest {

    private static final int KEY_Y = 89;
    private static final int KEY_LEFT_CONTROL = 341;
    private static final int MOD_CONTROL = 2;

    @Test
    void dispatchTemporarilyNeutralizesMatchingBindings() {
        var matching = new RecordingBinding(true);
        var nonMatching = new RecordingBinding(false);
        var dispatcherSawNeutralizedBinding = new AtomicBoolean(false);
        var dispatch = new ModifiedKeybindingDispatch(() -> List.of(matching, nonMatching));

        var dispatched = dispatch.dispatch(KEY_Y, MOD_CONTROL, () ->
                dispatcherSawNeutralizedBinding.set(matching.neutralized && !nonMatching.neutralized)
        );

        assertTrue(dispatched);
        assertTrue(dispatcherSawNeutralizedBinding.get());
        assertFalse(matching.neutralized);
        assertFalse(nonMatching.neutralized);
        assertEquals(1, matching.runs);
        assertEquals(0, nonMatching.runs);
    }

    @Test
    void dispatchSkipsWhenNoModifiedBindingMatches() {
        var binding = new RecordingBinding(false);
        var dispatcherCalled = new AtomicBoolean(false);
        var dispatch = new ModifiedKeybindingDispatch(() -> List.of(binding));

        var dispatched = dispatch.dispatch(KEY_Y, MOD_CONTROL, () ->
                dispatcherCalled.set(true)
        );

        assertFalse(dispatched);
        assertFalse(dispatcherCalled.get());
        assertEquals(0, binding.runs);
    }

    @Test
    void dispatchSkipsModifierKeyEvents() {
        var binding = new RecordingBinding(true);
        var dispatcherCalled = new AtomicBoolean(false);
        var dispatch = new ModifiedKeybindingDispatch(() -> List.of(binding));

        var dispatched = dispatch.dispatch(KEY_LEFT_CONTROL, MOD_CONTROL, () ->
                dispatcherCalled.set(true)
        );

        assertFalse(dispatched);
        assertFalse(dispatcherCalled.get());
        assertEquals(0, binding.runs);
    }

    private static final class RecordingBinding implements ModifiedKeybindingDispatch.Binding {

        private final boolean matches;
        private boolean neutralized;
        private int runs;

        private RecordingBinding(boolean matches) {
            this.matches = matches;
        }

        @Override
        public boolean matches(int keyCode, int modifiers) {
            return matches;
        }

        @Override
        public void runWithNeutralModifier(Runnable runnable) {
            runs++;
            neutralized = true;
            try {
                runnable.run();
            } finally {
                neutralized = false;
            }
        }
    }
}
