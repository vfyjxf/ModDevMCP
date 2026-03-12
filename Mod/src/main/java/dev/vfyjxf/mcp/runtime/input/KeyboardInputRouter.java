package dev.vfyjxf.mcp.runtime.input;

import dev.vfyjxf.mcp.api.model.OperationResult;

final class KeyboardInputRouter {

    private KeyboardInputRouter() {
    }

    static OperationResult<Void> keyPress(
            InputCommand command,
            ScreenInput screenInput,
            FallbackInput fallbackInput
    ) {
        var handled = screenInput != null && (
                screenInput.keyPressed(command.keyCode(), command.scanCode(), command.modifiers())
                        || screenInput.keyReleased(command.keyCode(), command.scanCode(), command.modifiers())
        );
        if (handled) {
            return OperationResult.success(null);
        }
        fallbackInput.dispatchKeyPress(command.keyCode(), command.scanCode(), command.modifiers());
        return OperationResult.success(null);
    }

    interface ScreenInput {

        boolean keyPressed(int keyCode, int scanCode, int modifiers);

        boolean keyReleased(int keyCode, int scanCode, int modifiers);

        boolean charTyped(char character, int modifiers);
    }

    interface FallbackInput {

        void dispatchKeyPress(int keyCode, int scanCode, int modifiers);
    }
}
