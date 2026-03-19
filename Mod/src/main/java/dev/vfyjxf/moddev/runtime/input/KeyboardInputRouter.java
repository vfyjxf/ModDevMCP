package dev.vfyjxf.moddev.runtime.input;

import dev.vfyjxf.moddev.api.model.OperationResult;
import org.lwjgl.glfw.GLFW;

final class KeyboardInputRouter {

    private static final ModifierKey[] MODIFIER_KEYS = new ModifierKey[]{
            new ModifierKey(GLFW.GLFW_MOD_SHIFT, GLFW.GLFW_KEY_LEFT_SHIFT),
            new ModifierKey(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_LEFT_CONTROL),
            new ModifierKey(GLFW.GLFW_MOD_ALT, GLFW.GLFW_KEY_LEFT_ALT),
            new ModifierKey(GLFW.GLFW_MOD_SUPER, GLFW.GLFW_KEY_LEFT_SUPER)
    };

    private KeyboardInputRouter() {
    }

    static OperationResult<Void> keyPress(
            InputCommand command,
            ScreenInput screenInput,
            FallbackInput fallbackInput
    ) {
        return keyPress(command, screenInput, fallbackInput, null);
    }

    static OperationResult<Void> keyPress(
            InputCommand command,
            ScreenInput screenInput,
            FallbackInput fallbackInput,
            VirtualModifierState virtualModifierState
    ) {
        return keyClick(command, screenInput, fallbackInput, virtualModifierState);
    }

    static OperationResult<Void> keyClick(
            InputCommand command,
            ScreenInput screenInput,
            FallbackInput fallbackInput
    ) {
        return keyClick(command, screenInput, fallbackInput, null);
    }

    static OperationResult<Void> keyClick(
            InputCommand command,
            ScreenInput screenInput,
            FallbackInput fallbackInput,
            VirtualModifierState virtualModifierState
    ) {
        if (command.modifiers() != 0) {
            dispatchModifiedKeyPress(command, fallbackInput, virtualModifierState);
            return OperationResult.success(null);
        }
        dispatchPlainKeyPress(command, fallbackInput, virtualModifierState);
        return OperationResult.success(null);
    }

    static OperationResult<Void> keyDown(InputCommand command, FallbackInput fallbackInput) {
        return keyDown(command, fallbackInput, null);
    }

    static OperationResult<Void> keyDown(
            InputCommand command,
            FallbackInput fallbackInput,
            VirtualModifierState virtualModifierState
    ) {
        if (virtualModifierState != null && isModifierKey(command.keyCode())) {
            virtualModifierState.keyDown(command.keyCode());
        }
        fallbackInput.dispatchKeyDown(
                command.keyCode(),
                command.scanCode(),
                mergedModifiers(command.modifiers(), virtualModifierState)
        );
        return OperationResult.success(null);
    }

    static OperationResult<Void> keyUp(InputCommand command, FallbackInput fallbackInput) {
        return keyUp(command, fallbackInput, null);
    }

    static OperationResult<Void> keyUp(
            InputCommand command,
            FallbackInput fallbackInput,
            VirtualModifierState virtualModifierState
    ) {
        if (virtualModifierState != null && isModifierKey(command.keyCode())) {
            virtualModifierState.keyUp(command.keyCode());
        }
        fallbackInput.dispatchKeyUp(
                command.keyCode(),
                command.scanCode(),
                mergedModifiers(command.modifiers(), virtualModifierState)
        );
        return OperationResult.success(null);
    }

    /**
     * Applies currently held virtual modifiers on top of per-command modifier bits.
     */
    static int mergedModifiers(int commandModifiers, VirtualModifierState virtualModifierState) {
        if (virtualModifierState == null) {
            return commandModifiers;
        }
        return commandModifiers | virtualModifierState.modifierBits();
    }

    private static void dispatchPlainKeyPress(
            InputCommand command,
            FallbackInput fallbackInput,
            VirtualModifierState virtualModifierState
    ) {
        var modifiers = mergedModifiers(command.modifiers(), virtualModifierState);
        fallbackInput.dispatchKeyDown(command.keyCode(), command.scanCode(), modifiers);
        fallbackInput.dispatchKeyUp(command.keyCode(), command.scanCode(), modifiers);
    }

    private static void dispatchModifiedKeyPress(
            InputCommand command,
            FallbackInput fallbackInput,
            VirtualModifierState virtualModifierState
    ) {
        var activeModifiers = virtualModifierState == null ? 0 : virtualModifierState.modifierBits();
        for (ModifierKey modifierKey : MODIFIER_KEYS) {
            if ((command.modifiers() & modifierKey.mask()) == 0) {
                continue;
            }
            activeModifiers |= modifierKey.mask();
            fallbackInput.dispatchKeyDown(modifierKey.keyCode(), 0, activeModifiers);
        }
        var modifiers = mergedModifiers(command.modifiers(), virtualModifierState);
        fallbackInput.dispatchKeyDown(command.keyCode(), command.scanCode(), modifiers);
        fallbackInput.dispatchKeyUp(command.keyCode(), command.scanCode(), modifiers);
        for (int index = MODIFIER_KEYS.length - 1; index >= 0; index--) {
            var modifierKey = MODIFIER_KEYS[index];
            if ((command.modifiers() & modifierKey.mask()) == 0) {
                continue;
            }
            fallbackInput.dispatchKeyUp(modifierKey.keyCode(), 0, activeModifiers);
            activeModifiers &= ~modifierKey.mask();
        }
    }

    private static boolean isModifierKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
                    GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
                    GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,
                    GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> true;
            default -> false;
        };
    }

    interface ScreenInput {

        boolean keyPressed(int keyCode, int scanCode, int modifiers);

        boolean keyReleased(int keyCode, int scanCode, int modifiers);

        boolean charTyped(char character, int modifiers);
    }

    interface FallbackInput {

        void dispatchKeyDown(int keyCode, int scanCode, int modifiers);

        void dispatchKeyUp(int keyCode, int scanCode, int modifiers);
    }

    private record ModifierKey(int mask, int keyCode) {
    }
}

