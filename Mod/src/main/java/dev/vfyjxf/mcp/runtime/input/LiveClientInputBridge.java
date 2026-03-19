package dev.vfyjxf.mcp.runtime.input;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.ClientScreenMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class LiveClientInputBridge implements ClientInputBridge {

    private static final long EXECUTION_TIMEOUT_SECONDS = 5L;
    private final VirtualModifierState virtualModifierState = new VirtualModifierState();

    @Override
    public ClientScreenMetrics metrics() {
        var minecraft = Minecraft.getInstance();
        return new ClientScreenMetrics(
                minecraft.screen == null ? null : minecraft.screen.getClass().getName(),
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight()
        );
    }

    @Override
    public OperationResult<Void> execute(InputCommand command) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            return executeOnClient(command);
        }
        var future = new CompletableFuture<OperationResult<Void>>();
        minecraft.execute(() -> future.complete(executeOnClient(command)));
        try {
            return future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return OperationResult.rejected("interrupted while waiting for client input execution");
        } catch (ExecutionException exception) {
            return OperationResult.rejected("client input execution failed: " + exception.getCause().getMessage());
        } catch (TimeoutException exception) {
            return OperationResult.rejected("timed out waiting for client input execution");
        }
    }

    private OperationResult<Void> executeOnClient(InputCommand command) {
        var screen = Minecraft.getInstance().screen;
        return switch (command.action()) {
            case "click" -> click(screen, command);
            case "move" -> move(screen, command);
            case "hover" -> hover(screen, command);
            case "mouse_down" -> mouseDown(command);
            case "mouse_up" -> mouseUp(command);
            case "key_press", "key_click" -> keyClick(screen, command);
            case "key_down" -> keyDown(command);
            case "key_up" -> keyUp(command);
            case "type_text" -> typeText(screen, command);
            default -> OperationResult.rejected("unsupported input action: " + command.action());
        };
    }

    private OperationResult<Void> move(Screen screen, InputCommand command) {
        if (screen == null) {
            return OperationResult.rejected("game_unavailable: no active client screen");
        }
        screen.mouseMoved(command.x(), command.y());
        return OperationResult.success(null);
    }

    private OperationResult<Void> hover(Screen screen, InputCommand command) {
        if (screen == null) {
            return OperationResult.rejected("game_unavailable: no active client screen");
        }
        screen.mouseMoved(command.x(), command.y());
        if (command.durationMs() > 0) {
            try {
                Thread.sleep(command.durationMs());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return OperationResult.rejected("interrupted while waiting for hover delay");
            }
        }
        return OperationResult.success(null);
    }

    private OperationResult<Void> click(Screen screen, InputCommand command) {
        if (screen == null) {
            return OperationResult.rejected("game_unavailable: no active client screen");
        }
        screen.mouseMoved(command.x(), command.y());
        var clicked = screen.mouseClicked(command.x(), command.y(), command.button());
        var released = screen.mouseReleased(command.x(), command.y(), command.button());
        return clicked || released
                ? OperationResult.success(null)
                : OperationResult.rejected("click was not handled by current screen");
    }

    private OperationResult<Void> keyClick(Screen screen, InputCommand command) {
        var minecraft = Minecraft.getInstance();
        var effectiveModifiers = KeyboardInputRouter.mergedModifiers(command.modifiers(), virtualModifierState);
        if (screen != null) {
            var focusedTextInput = focusedTextInput(screen);
            if (focusedTextInput != null && FocusedTextShortcutHandler.tryHandle(
                    command.keyCode(),
                    effectiveModifiers,
                    focusedTextInput,
                    new MinecraftClipboardAccess(minecraft)
            )) {
                return OperationResult.success(null);
            }
        }
        return KeyboardInputRouter.keyClick(
                command,
                screen == null ? null : new ScreenKeyboardInput(screen),
                new MinecraftKeyboardInput(minecraft, virtualModifierState),
                virtualModifierState
        );
    }

    private OperationResult<Void> keyDown(InputCommand command) {
        return KeyboardInputRouter.keyDown(
                command,
                new MinecraftKeyboardInput(Minecraft.getInstance(), virtualModifierState),
                virtualModifierState
        );
    }

    private OperationResult<Void> keyUp(InputCommand command) {
        return KeyboardInputRouter.keyUp(
                command,
                new MinecraftKeyboardInput(Minecraft.getInstance(), virtualModifierState),
                virtualModifierState
        );
    }

    private OperationResult<Void> mouseDown(InputCommand command) {
        return mouseButton(command, GLFW.GLFW_PRESS);
    }

    private OperationResult<Void> mouseUp(InputCommand command) {
        return mouseButton(command, GLFW.GLFW_RELEASE);
    }

    private OperationResult<Void> mouseButton(InputCommand command, int action) {
        var minecraft = Minecraft.getInstance();
        var windowHandle = minecraft.getWindow().getWindow();
        try {
            // Keep raw mouse button injection coordinate-aware so callers can build drag-style
            // sequences out of move/down/up primitives without relying on stale cursor state.
            var onMove = minecraft.mouseHandler.getClass().getDeclaredMethod(
                    "onMove",
                    long.class,
                    double.class,
                    double.class
            );
            onMove.setAccessible(true);
            onMove.invoke(minecraft.mouseHandler, windowHandle, command.x(), command.y());

            var onPress = minecraft.mouseHandler.getClass().getDeclaredMethod(
                    "onPress",
                    long.class,
                    int.class,
                    int.class,
                    int.class
            );
            onPress.setAccessible(true);
            onPress.invoke(minecraft.mouseHandler, windowHandle, command.button(), action, command.modifiers());
            return OperationResult.success(null);
        } catch (ReflectiveOperationException exception) {
            return OperationResult.rejected("mouse button injection failed: " + exception.getMessage());
        }
    }

    private FocusedTextShortcutHandler.FocusedTextInput focusedTextInput(Screen screen) {
        var focused = screen.getFocused();
        if (focused instanceof EditBox editBox) {
            return new EditBoxFocusedTextInput(editBox);
        }
        if (focused instanceof MultiLineEditBox editBox) {
            return MultiLineEditBoxFocusedTextInput.create(editBox);
        }
        return null;
    }

    private OperationResult<Void> typeText(Screen screen, InputCommand command) {
        if (screen == null) {
            return OperationResult.rejected("game_unavailable: no active client screen");
        }
        if (command.text() == null || command.text().isBlank()) {
            return OperationResult.rejected("type_text requires non-empty text");
        }
        var handled = false;
        for (char character : command.text().toCharArray()) {
            handled = screen.charTyped(character, command.modifiers()) || handled;
        }
        return handled
                ? OperationResult.success(null)
                : OperationResult.rejected("type_text was not handled by current screen");
    }

    private record ScreenKeyboardInput(Screen screen) implements KeyboardInputRouter.ScreenInput {

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return screen.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            return screen.keyReleased(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char character, int modifiers) {
            return screen.charTyped(character, modifiers);
        }
    }

    private static final class MinecraftKeyboardInput implements KeyboardInputRouter.FallbackInput {

        private final Minecraft minecraft;
        private final ModifiedKeybindingDispatch modifiedKeybindingDispatch;

        private MinecraftKeyboardInput(Minecraft minecraft, VirtualModifierState virtualModifierState) {
            this(minecraft, ModifiedKeybindingDispatch.live(virtualModifierState::modifierBits));
        }

        private MinecraftKeyboardInput(Minecraft minecraft, ModifiedKeybindingDispatch modifiedKeybindingDispatch) {
            this.minecraft = minecraft;
            this.modifiedKeybindingDispatch = modifiedKeybindingDispatch;
        }

        @Override
        public void dispatchKeyDown(int keyCode, int scanCode, int modifiers) {
            var windowHandle = minecraft.getWindow().getWindow();
            minecraft.keyboardHandler.keyPress(windowHandle, keyCode, scanCode, GLFW.GLFW_PRESS, modifiers);
            modifiedKeybindingDispatch.dispatch(keyCode, modifiers,
                    () -> ClientHooks.onKeyInput(keyCode, scanCode, GLFW.GLFW_PRESS, modifiers));
        }

        @Override
        public void dispatchKeyUp(int keyCode, int scanCode, int modifiers) {
            var windowHandle = minecraft.getWindow().getWindow();
            minecraft.keyboardHandler.keyPress(windowHandle, keyCode, scanCode, GLFW.GLFW_RELEASE, modifiers);
            modifiedKeybindingDispatch.dispatch(keyCode, modifiers,
                    () -> ClientHooks.onKeyInput(keyCode, scanCode, GLFW.GLFW_RELEASE, modifiers));
        }
    }

    private record MinecraftClipboardAccess(Minecraft minecraft) implements FocusedTextShortcutHandler.ClipboardAccess {

        @Override
        public String getClipboard() {
            return minecraft.keyboardHandler.getClipboard();
        }

        @Override
        public void setClipboard(String text) {
            minecraft.keyboardHandler.setClipboard(text == null ? "" : text);
        }
    }

    private static final class EditBoxFocusedTextInput implements FocusedTextShortcutHandler.FocusedTextInput {

        private final EditBox editBox;

        private EditBoxFocusedTextInput(EditBox editBox) {
            this.editBox = editBox;
        }

        @Override
        public void selectAll() {
            editBox.moveCursorToEnd(false);
            editBox.setHighlightPos(0);
        }

        @Override
        public String selectedText() {
            return editBox.getHighlighted();
        }

        @Override
        public void insertText(String text) {
            editBox.insertText(text);
        }

        @Override
        public boolean editable() {
            return readBooleanField(editBox, "isEditable");
        }
    }

    private static final class MultiLineEditBoxFocusedTextInput implements FocusedTextShortcutHandler.FocusedTextInput {

        private final Object textField;
        private final Object whenceAbsolute;
        private final Object whenceEnd;

        private MultiLineEditBoxFocusedTextInput(Object textField, Object whenceAbsolute, Object whenceEnd) {
            this.textField = textField;
            this.whenceAbsolute = whenceAbsolute;
            this.whenceEnd = whenceEnd;
        }

        static MultiLineEditBoxFocusedTextInput create(MultiLineEditBox editBox) {
            try {
                var textFieldField = MultiLineEditBox.class.getDeclaredField("textField");
                textFieldField.setAccessible(true);
                var textField = textFieldField.get(editBox);
                var whenceClass = Class.forName("net.minecraft.client.gui.components.MultilineTextField$Whence");
                @SuppressWarnings("unchecked")
                var enumClass = (Class<? extends Enum>) whenceClass;
                return new MultiLineEditBoxFocusedTextInput(
                        textField,
                        Enum.valueOf(enumClass, "ABSOLUTE"),
                        Enum.valueOf(enumClass, "END")
                );
            } catch (ReflectiveOperationException exception) {
                return null;
            }
        }

        @Override
        public void selectAll() {
            invoke(textField, "seekCursor", whenceEnd.getClass(), whenceEnd, int.class, 0);
            invoke(textField, "setSelecting", boolean.class, true);
            invoke(textField, "seekCursor", whenceAbsolute.getClass(), whenceAbsolute, int.class, 0);
            invoke(textField, "setSelecting", boolean.class, false);
        }

        @Override
        public String selectedText() {
            return (String) invoke(textField, "getSelectedText");
        }

        @Override
        public void insertText(String text) {
            invoke(textField, "insertText", String.class, text);
        }

        @Override
        public boolean editable() {
            return true;
        }
    }

    private static Object invoke(Object target, String methodName, Object... args) {
        Class<?>[] parameterTypes = new Class<?>[args.length / 2];
        Object[] values = new Object[args.length / 2];
        for (int index = 0; index < args.length; index += 2) {
            parameterTypes[index / 2] = (Class<?>) args[index];
            values[index / 2] = args[index + 1];
        }
        try {
            var method = target.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, values);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke " + methodName + " on " + target.getClass().getName(), exception);
        }
    }

    private static Object invoke(Object target, String methodName) {
        try {
            var method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke " + methodName + " on " + target.getClass().getName(), exception);
        }
    }

    private static boolean readBooleanField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getBoolean(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read " + fieldName + " on " + target.getClass().getName(), exception);
        }
    }
}
