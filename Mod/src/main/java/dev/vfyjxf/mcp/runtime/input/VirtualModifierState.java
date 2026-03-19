package dev.vfyjxf.mcp.runtime.input;

import org.lwjgl.glfw.GLFW;

/**
 * Tracks agent-driven modifier keys as logical held state.
 *
 * <p>Left/right variants are normalized into one logical modifier so callers
 * can reason about a single shift/control/alt/super state.</p>
 */
final class VirtualModifierState {

    private boolean shiftActive;
    private boolean controlActive;
    private boolean altActive;
    private boolean superActive;

    void keyDown(int keyCode) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> shiftActive = true;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> controlActive = true;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> altActive = true;
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> superActive = true;
            default -> {
            }
        }
    }

    void keyUp(int keyCode) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> shiftActive = false;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> controlActive = false;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> altActive = false;
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> superActive = false;
            default -> {
            }
        }
    }

    int modifierBits() {
        var modifiers = 0;
        if (shiftActive) {
            modifiers |= GLFW.GLFW_MOD_SHIFT;
        }
        if (controlActive) {
            modifiers |= GLFW.GLFW_MOD_CONTROL;
        }
        if (altActive) {
            modifiers |= GLFW.GLFW_MOD_ALT;
        }
        if (superActive) {
            modifiers |= GLFW.GLFW_MOD_SUPER;
        }
        return modifiers;
    }

    boolean shiftActive() {
        return shiftActive;
    }

    boolean controlActive() {
        return controlActive;
    }

    boolean altActive() {
        return altActive;
    }

    boolean superActive() {
        return superActive;
    }

    void clear() {
        shiftActive = false;
        controlActive = false;
        altActive = false;
        superActive = false;
    }
}
