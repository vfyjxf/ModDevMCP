package dev.vfyjxf.mcp.runtime.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualModifierStateTest {

    // Mirror org.lwjgl.glfw.GLFW constants so tests can stay readable without
    // requiring lwjgl on the test runtime classpath.
    private static final int GLFW_KEY_LEFT_SHIFT = 340;
    private static final int GLFW_KEY_RIGHT_SHIFT = 344;
    private static final int GLFW_KEY_LEFT_CONTROL = 341;
    private static final int GLFW_KEY_RIGHT_CONTROL = 345;
    private static final int GLFW_KEY_LEFT_ALT = 342;
    private static final int GLFW_MOD_SHIFT = 0x0001;
    private static final int GLFW_MOD_CONTROL = 0x0002;
    private static final int GLFW_MOD_ALT = 0x0004;

    @Test
    void leftAndRightShiftShareOneLogicalState() {
        var state = new VirtualModifierState();

        state.keyDown(GLFW_KEY_LEFT_SHIFT);
        assertTrue(state.shiftActive());

        state.keyUp(GLFW_KEY_RIGHT_SHIFT);
        assertFalse(state.shiftActive());
    }

    @Test
    void modifierBitsReflectHeldShiftControlAndAlt() {
        var state = new VirtualModifierState();

        state.keyDown(GLFW_KEY_LEFT_SHIFT);
        state.keyDown(GLFW_KEY_RIGHT_CONTROL);
        state.keyDown(GLFW_KEY_LEFT_ALT);

        assertEquals(GLFW_MOD_SHIFT | GLFW_MOD_CONTROL | GLFW_MOD_ALT, state.modifierBits());
    }

    @Test
    void controlPathUsesLeftAndRightKeysAsOneLogicalModifier() {
        var state = new VirtualModifierState();

        state.keyDown(GLFW_KEY_RIGHT_CONTROL);
        assertTrue(state.controlActive());

        state.keyUp(GLFW_KEY_LEFT_CONTROL);
        assertFalse(state.controlActive());
    }

    @Test
    void clearResetsAllHeldStateAndModifierBits() {
        var state = new VirtualModifierState();

        state.keyDown(GLFW_KEY_LEFT_SHIFT);
        state.keyDown(GLFW_KEY_RIGHT_CONTROL);
        state.keyDown(GLFW_KEY_LEFT_ALT);

        state.clear();

        assertFalse(state.shiftActive());
        assertFalse(state.controlActive());
        assertFalse(state.altActive());
        assertFalse(state.superActive());
        assertEquals(0, state.modifierBits());
    }
}
