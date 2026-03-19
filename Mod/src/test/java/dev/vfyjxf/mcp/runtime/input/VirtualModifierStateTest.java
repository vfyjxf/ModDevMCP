package dev.vfyjxf.mcp.runtime.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualModifierStateTest {

    private static final int KEY_LEFT_SHIFT = 340;
    private static final int KEY_RIGHT_SHIFT = 344;

    @Test
    void leftAndRightShiftShareOneLogicalState() {
        var state = new VirtualModifierState();

        state.keyDown(KEY_LEFT_SHIFT);
        assertTrue(state.shiftActive());

        state.keyUp(KEY_RIGHT_SHIFT);
        assertFalse(state.shiftActive());
    }
}
