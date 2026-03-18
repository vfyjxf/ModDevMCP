package dev.vfyjxf.mcp.runtime.input;

import dev.vfyjxf.mcp.api.model.OperationResult;
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
        return keyClick(command, screenInput, fallbackInput);
    }

    static OperationResult<Void> keyClick(
            InputCommand command,
            ScreenInput screenInput,
            FallbackInput fallbackInput
    ) {
        if (command.modifiers() != 0) {
            dispatchModifiedKeyPress(command, fallbackInput);
            return OperationResult.success(null);
        }
        dispatchPlainKeyPress(command, fallbackInput);
        return OperationResult.success(null);
    }

    static OperationResult<Void> keyDown(InputCommand command, FallbackInput fallbackInput) {
        fallbackInput.dispatchKeyDown(command.keyCode(), command.scanCode(), command.modifiers());
        return OperationResult.success(null);
    }

    static OperationResult<Void> keyUp(InputCommand command, FallbackInput fallbackInput) {
        fallbackInput.dispatchKeyUp(command.keyCode(), command.scanCode(), command.modifiers());
        return OperationResult.success(null);
    }

    private static void dispatchPlainKeyPress(InputCommand command, FallbackInput fallbackInput) {
        fallbackInput.dispatchKeyDown(command.keyCode(), command.scanCode(), command.modifiers());
        fallbackInput.dispatchKeyUp(command.keyCode(), command.scanCode(), command.modifiers());
    }

    private static void dispatchModifiedKeyPress(InputCommand command, FallbackInput fallbackInput) {
        var activeModifiers = 0;
        for (ModifierKey modifierKey : MODIFIER_KEYS) {
            if ((command.modifiers() & modifierKey.mask()) == 0) {
                continue;
            }
            activeModifiers |= modifierKey.mask();
            fallbackInput.dispatchKeyDown(modifierKey.keyCode(), 0, activeModifiers);
        }
        fallbackInput.dispatchKeyDown(command.keyCode(), command.scanCode(), command.modifiers());
        fallbackInput.dispatchKeyUp(command.keyCode(), command.scanCode(), command.modifiers());
        for (int index = MODIFIER_KEYS.length - 1; index >= 0; index--) {
            var modifierKey = MODIFIER_KEYS[index];
            if ((command.modifiers() & modifierKey.mask()) == 0) {
                continue;
            }
            fallbackInput.dispatchKeyUp(modifierKey.keyCode(), 0, activeModifiers);
            activeModifiers &= ~modifierKey.mask();
        }
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
