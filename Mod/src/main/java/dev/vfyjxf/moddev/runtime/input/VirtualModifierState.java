package dev.vfyjxf.moddev.runtime.input;

import org.lwjgl.glfw.GLFW;

/**
 * Tracks agent-driven modifier keys as logical held state.
 *
 * <p>Left/right variants are normalized into one logical modifier so callers
 * can reason about a single shift/control/alt/super state.</p>
 */
public final class VirtualModifierState {

    private static final VirtualModifierState GLOBAL = new VirtualModifierState();

    private boolean shiftActive;
    private boolean controlActive;
    private boolean altActive;
    private boolean superActive;

    /**
     * Returns shared runtime modifier state used by lifecycle resets and query hooks.
     */
    public static VirtualModifierState global() {
        return GLOBAL;
    }

    /**
     * Clears the shared runtime modifier state at client lifecycle boundaries.
     */
    public static void resetGlobalForClientLifecycle() {
        GLOBAL.clear();
    }

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

    /**
     * Returns the current held virtual modifier bits in GLFW modifier-mask form.
     */
    public int modifierBits() {
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

    /**
     * Returns whether virtual SHIFT is currently held.
     */
    public boolean shiftActive() {
        return shiftActive;
    }

    /**
     * Returns whether virtual CONTROL is currently held.
     */
    public boolean controlActive() {
        return controlActive;
    }

    /**
     * Returns whether virtual ALT is currently held.
     */
    public boolean altActive() {
        return altActive;
    }

    /**
     * Returns whether virtual SUPER is currently held.
     */
    public boolean superActive() {
        return superActive;
    }

    /**
     * Clears all held virtual modifier state.
     */
    public void clear() {
        shiftActive = false;
        controlActive = false;
        altActive = false;
        superActive = false;
    }
}

