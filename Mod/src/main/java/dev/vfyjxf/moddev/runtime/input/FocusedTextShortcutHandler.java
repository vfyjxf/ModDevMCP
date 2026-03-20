package dev.vfyjxf.moddev.runtime.input;

import org.lwjgl.glfw.GLFW;

final class FocusedTextShortcutHandler {

    private static final int PRIMARY_MODIFIER_MASK = GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER;

    private FocusedTextShortcutHandler() {
    }

    static boolean tryHandle(InputCommand command, FocusedTextInput focusedTextInput, ClipboardAccess clipboardAccess) {
        return tryHandle(command.keyCode(), command.modifiers(), focusedTextInput, clipboardAccess);
    }

    static boolean tryHandle(
            int keyCode,
            int modifiers,
            FocusedTextInput focusedTextInput,
            ClipboardAccess clipboardAccess
    ) {
        if (focusedTextInput == null || clipboardAccess == null) {
            return false;
        }
        if (!isPrimaryShortcut(modifiers)) {
            return false;
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_A -> {
                focusedTextInput.selectAll();
                yield true;
            }
            case GLFW.GLFW_KEY_C -> {
                clipboardAccess.setClipboard(focusedTextInput.selectedText());
                yield true;
            }
            case GLFW.GLFW_KEY_V -> {
                if (!focusedTextInput.editable()) {
                    yield false;
                }
                focusedTextInput.insertText(clipboardAccess.getClipboard());
                yield true;
            }
            case GLFW.GLFW_KEY_X -> {
                if (!focusedTextInput.editable()) {
                    yield false;
                }
                clipboardAccess.setClipboard(focusedTextInput.selectedText());
                focusedTextInput.insertText("");
                yield true;
            }
            default -> false;
        };
    }

    private static boolean isPrimaryShortcut(int modifiers) {
        return (modifiers & PRIMARY_MODIFIER_MASK) != 0
                && (modifiers & GLFW.GLFW_MOD_ALT) == 0
                && (modifiers & GLFW.GLFW_MOD_SHIFT) == 0;
    }

    interface FocusedTextInput {

        void selectAll();

        String selectedText();

        void insertText(String text);

        boolean editable();
    }

    interface ClipboardAccess {

        String getClipboard();

        void setClipboard(String text);
    }
}

