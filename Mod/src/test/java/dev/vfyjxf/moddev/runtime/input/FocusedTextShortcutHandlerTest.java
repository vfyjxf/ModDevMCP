package dev.vfyjxf.moddev.runtime.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FocusedTextShortcutHandlerTest {

    private static final int KEY_A = 65;
    private static final int KEY_V = 86;
    private static final int MOD_CONTROL = 2;

    @Test
    void controlASelectsAllSoNextInsertReplacesWholeValue() {
        var input = new RecordingFocusedTextInput("abc");
        var clipboard = new RecordingClipboardAccess();

        var handled = FocusedTextShortcutHandler.tryHandle(
                new InputCommand("key_press", 0.0d, 0.0d, 0, KEY_A, 0, MOD_CONTROL, null, 0),
                input,
                clipboard
        );
        input.insertText("z");

        assertTrue(handled);
        assertEquals("z", input.value());
    }

    @Test
    void controlVRespectsEditableFlag() {
        var input = new RecordingFocusedTextInput("abc");
        input.editable = false;
        var clipboard = new RecordingClipboardAccess();
        clipboard.value = "z";

        var handled = FocusedTextShortcutHandler.tryHandle(
                new InputCommand("key_press", 0.0d, 0.0d, 0, KEY_V, 0, MOD_CONTROL, null, 0),
                input,
                clipboard
        );

        assertFalse(handled);
        assertEquals("abc", input.value());
    }

    @Test
    void persistedPrimaryModifierCanTriggerShortcutEvenWhenCommandModifierIsEmpty() {
        var input = new RecordingFocusedTextInput("abc");
        var clipboard = new RecordingClipboardAccess();

        var handled = FocusedTextShortcutHandler.tryHandle(
                KEY_A,
                MOD_CONTROL,
                input,
                clipboard
        );
        input.insertText("z");

        assertTrue(handled);
        assertEquals("z", input.value());
    }

    private static final class RecordingFocusedTextInput implements FocusedTextShortcutHandler.FocusedTextInput {

        private String value;
        private int selectionStart;
        private int selectionEnd;
        private boolean editable = true;

        private RecordingFocusedTextInput(String value) {
            this.value = value;
            this.selectionStart = value.length();
            this.selectionEnd = value.length();
        }

        @Override
        public void selectAll() {
            selectionStart = 0;
            selectionEnd = value.length();
        }

        @Override
        public String selectedText() {
            return value.substring(Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd));
        }

        @Override
        public void insertText(String text) {
            var start = Math.min(selectionStart, selectionEnd);
            var end = Math.max(selectionStart, selectionEnd);
            value = value.substring(0, start) + text + value.substring(end);
            selectionStart = start + text.length();
            selectionEnd = selectionStart;
        }

        @Override
        public boolean editable() {
            return editable;
        }

        private String value() {
            return value;
        }
    }

    private static final class RecordingClipboardAccess implements FocusedTextShortcutHandler.ClipboardAccess {

        private String value = "";

        @Override
        public String getClipboard() {
            return value;
        }

        @Override
        public void setClipboard(String text) {
            value = text;
        }
    }
}

