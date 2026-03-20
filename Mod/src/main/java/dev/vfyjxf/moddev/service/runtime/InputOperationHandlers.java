package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Exposes low-level client input operations through the HTTP operation catalog.
 */
public final class InputOperationHandlers {

    private InputOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeRegistries registries) {
        Objects.requireNonNull(registries, "registries");
        var dispatcher = new InputActionDispatcher(registries);
        return List.of(
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "input.action",
                                "input",
                                "Input Action",
                                "Dispatches a low-level keyboard or mouse action to the live client.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.ofEntries(
                                                Map.entry("action", Map.of("type", "string")),
                                                Map.entry("screenClass", Map.of("type", "string")),
                                                Map.entry("coordinateSpace", Map.of("type", "string")),
                                                Map.entry("x", Map.of("type", "number")),
                                                Map.entry("y", Map.of("type", "number")),
                                                Map.entry("button", Map.of("type", "integer")),
                                                Map.entry("hoverDelayMs", Map.of("type", "integer")),
                                                Map.entry("keyCode", Map.of("type", "integer")),
                                                Map.entry("scanCode", Map.of("type", "integer")),
                                                Map.entry("modifiers", Map.of("type", "integer")),
                                                Map.entry("text", Map.of("type", "string")),
                                                Map.entry("intent", Map.of("type", "string"))
                                        ),
                                        List.of("action")
                                ),
                                Map.of(
                                        "operationId", "input.action",
                                        "targetSide", "client",
                                        "input", Map.of(
                                                "action", "key_press",
                                                "keyCode", 69
                                        )
                                )
                        ),
                        (input, resolvedTargetSide) -> {
                            var action = input.get("action");
                            if (!(action instanceof String actionName) || actionName.isBlank()) {
                                throw RuntimeOperationBindings.executionFailure("invalid_input: missing action");
                            }
                            var dispatchResult = dispatcher.dispatch(actionName, Map.copyOf(input));
                            if (!dispatchResult.success()) {
                                throw RuntimeOperationBindings.executionFailure(dispatchResult.error());
                            }
                            return dispatchResult.payload(actionName);
                        }
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "input.clipboard_set",
                                "input",
                                "Set Clipboard",
                                "Updates the live client clipboard.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of("text", Map.of("type", "string")),
                                        List.of("text")
                                ),
                                Map.of(
                                        "operationId", "input.clipboard_set",
                                        "targetSide", "client",
                                        "input", Map.of("text", "hello from ModDevMCP")
                                )
                        ),
                        (input, resolvedTargetSide) -> {
                            var text = input.get("text");
                            if (!(text instanceof String value)) {
                                throw RuntimeOperationBindings.executionFailure("invalid_input: missing text");
                            }
                            if (!setClipboard(value)) {
                                throw RuntimeOperationBindings.executionFailure("clipboard_unavailable");
                            }
                            return Map.of(
                                    "text", value,
                                    "updated", true
                            );
                        }
                )
        );
    }

    private static boolean setClipboard(String text) {
        try {
            var minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            var instance = minecraftClass.getMethod("getInstance").invoke(null);
            if (instance == null) {
                return false;
            }
            var keyboardHandler = findMemberValue(instance, "keyboardHandler", "getKeyboardHandler");
            if (keyboardHandler == null) {
                return false;
            }
            var setter = findClipboardSetter(keyboardHandler.getClass());
            if (setter == null) {
                return false;
            }
            setter.invoke(keyboardHandler, text);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        }
    }

    private static Object findMemberValue(Object instance, String fieldName, String accessorName) {
        try {
            var field = instance.getClass().getField(fieldName);
            return field.get(instance);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            var method = instance.getClass().getMethod(accessorName);
            return method.invoke(instance);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method findClipboardSetter(Class<?> keyboardHandlerClass) {
        for (var method : keyboardHandlerClass.getMethods()) {
            if (!"setClipboard".equals(method.getName())) {
                continue;
            }
            var parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0] == String.class) {
                return method;
            }
        }
        return null;
    }
}
