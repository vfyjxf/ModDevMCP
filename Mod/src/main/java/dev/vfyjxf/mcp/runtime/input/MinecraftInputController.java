package dev.vfyjxf.mcp.runtime.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.ClientScreenMetrics;
import dev.vfyjxf.mcp.api.runtime.InputController;
import dev.vfyjxf.mcp.runtime.ui.UiPointerStateRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.Objects;

public final class MinecraftInputController implements InputController {

    private final ClientInputBridge bridge;
    private final UiPointerStateRegistry pointerStates;
    private final UiIntentKeyResolver intentKeyResolver;

    public MinecraftInputController() {
        this(new LiveClientInputBridge(), new UiPointerStateRegistry(), liveIntentKeyResolver());
    }

    public MinecraftInputController(UiPointerStateRegistry pointerStates) {
        this(new LiveClientInputBridge(), pointerStates, liveIntentKeyResolver());
    }

    MinecraftInputController(ClientInputBridge bridge) {
        this(bridge, new UiPointerStateRegistry(), liveIntentKeyResolver());
    }

    MinecraftInputController(ClientInputBridge bridge, UiPointerStateRegistry pointerStates) {
        this(bridge, pointerStates, liveIntentKeyResolver());
    }

    MinecraftInputController(ClientInputBridge bridge, UiPointerStateRegistry pointerStates, UiIntentKeyResolver intentKeyResolver) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.pointerStates = Objects.requireNonNull(pointerStates, "pointerStates");
        this.intentKeyResolver = Objects.requireNonNull(intentKeyResolver, "intentKeyResolver");
    }

    @Override
    public OperationResult<Void> perform(String action, Map<String, Object> arguments) {
        if (action == null || action.isBlank()) {
            return OperationResult.rejected("missing input action");
        }
        try {
            InputCommand command;
            if ("ui_intent".equals(action)) {
                command = uiIntentCommand(arguments);
            } else {
                var metrics = bridge.metrics();
                var screenMismatch = validateScreen(arguments, metrics);
                if (screenMismatch != null) {
                    return screenMismatch;
                }
                command = switch (action) {
                    case "click" -> clickCommand(arguments, metrics);
                    case "move" -> moveCommand(arguments, metrics);
                    case "hover" -> hoverCommand(arguments, metrics);
                    case "key_press" -> keyPressCommand(arguments);
                    case "type_text" -> typeTextCommand(arguments);
                    default -> null;
                };
            }
            if (command == null) {
                return OperationResult.rejected("unsupported input action: " + action);
            }
            var metrics = bridge.metrics();
            var screenMismatch = validateScreen(arguments, metrics);
            if (screenMismatch != null) {
                return screenMismatch;
            }
            rememberPointer(metrics, command);
            return bridge.execute(command);
        } catch (UnsupportedOperationException exception) {
            return OperationResult.rejected(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return OperationResult.rejected(exception.getMessage());
        }
    }

    private OperationResult<Void> validateScreen(Map<String, Object> arguments, ClientScreenMetrics metrics) {
        var expectedScreenClass = stringArgument(arguments, "screenClass", null);
        if (expectedScreenClass == null || expectedScreenClass.isBlank()) {
            return null;
        }
        var actualScreenClass = metrics.screenClass();
        if (Objects.equals(expectedScreenClass, actualScreenClass)) {
            return null;
        }
        return OperationResult.rejected("screen_mismatch: expected " + expectedScreenClass + " but was " + actualScreenClass);
    }

    private InputCommand clickCommand(Map<String, Object> arguments, ClientScreenMetrics metrics) {
        var point = resolvePoint(arguments, metrics);
        return new InputCommand(
                "click",
                point.x(),
                point.y(),
                intArgument(arguments, "button", 0),
                0,
                0,
                intArgument(arguments, "modifiers", 0),
                null,
                0
        );
    }

    private InputCommand moveCommand(Map<String, Object> arguments, ClientScreenMetrics metrics) {
        var point = resolvePoint(arguments, metrics);
        return new InputCommand(
                "move",
                point.x(),
                point.y(),
                0,
                0,
                0,
                intArgument(arguments, "modifiers", 0),
                null,
                0
        );
    }

    private InputCommand hoverCommand(Map<String, Object> arguments, ClientScreenMetrics metrics) {
        var point = resolvePoint(arguments, metrics);
        return new InputCommand(
                "hover",
                point.x(),
                point.y(),
                0,
                0,
                0,
                intArgument(arguments, "modifiers", 0),
                null,
                intArgument(arguments, "hoverDelayMs", 100)
        );
    }

    private ResolvedPoint resolvePoint(Map<String, Object> arguments, ClientScreenMetrics metrics) {
        if (metrics.screenClass() == null || metrics.screenClass().isBlank()) {
            throw new IllegalArgumentException("game_unavailable: no active client screen");
        }
        var rawX = numberArgument(arguments, "x");
        var rawY = numberArgument(arguments, "y");
        var coordinateSpace = stringArgument(arguments, "coordinateSpace", "gui");
        var x = rawX.doubleValue();
        var y = rawY.doubleValue();
        if ("framebuffer".equals(coordinateSpace)) {
            x = x * metrics.guiWidth() / Math.max(1, metrics.framebufferWidth());
            y = y * metrics.guiHeight() / Math.max(1, metrics.framebufferHeight());
        } else if (!"gui".equals(coordinateSpace)) {
            throw new IllegalArgumentException("unsupported coordinate space: " + coordinateSpace);
        }
        return new ResolvedPoint(x, y);
    }

    private InputCommand keyPressCommand(Map<String, Object> arguments) {
        return new InputCommand(
                "key_press",
                0.0d,
                0.0d,
                0,
                intArgument(arguments, "keyCode", -1),
                intArgument(arguments, "scanCode", 0),
                intArgument(arguments, "modifiers", 0),
                null,
                0
        );
    }

    private InputCommand typeTextCommand(Map<String, Object> arguments) {
        return new InputCommand(
                "type_text",
                0.0d,
                0.0d,
                0,
                0,
                0,
                intArgument(arguments, "modifiers", 0),
                stringArgument(arguments, "text", ""),
                0
        );
    }

    private InputCommand uiIntentCommand(Map<String, Object> arguments) {
        var intent = stringArgument(arguments, "intent", null);
        if (intent == null || intent.isBlank()) {
            throw new IllegalArgumentException("missing ui intent");
        }
        var keyCode = intentKeyResolver.resolve(intent);
        if (keyCode < 0) {
            throw new UnsupportedOperationException("unsupported_intent");
        }
        return keyPressIntent(keyCode);
    }

    private static UiIntentKeyResolver liveIntentKeyResolver() {
        return intent -> {
            int resolved;
            try {
                resolved = switch (intent) {
                    case "inventory" -> keyValue(Minecraft.getInstance().options.keyInventory);
                    case "chat" -> keyValue(Minecraft.getInstance().options.keyChat);
                    case "pause_menu" -> GLFW.GLFW_KEY_ESCAPE;
                    default -> -1;
                };
            } catch (NoClassDefFoundError | ExceptionInInitializerError ignored) {
                resolved = -1;
            }
            return resolved >= 0 ? resolved : defaultKeyCodeForIntent(intent);
        };
    }

    private static int keyValue(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return -1;
        }
        return inputKeyValue(InputConstants.getKey(keyMapping.saveString()));
    }

    private static int inputKeyValue(Object key) {
        if (key == null) {
            return -1;
        }
        try {
            return ((Number) key.getClass().getMethod("getValue").invoke(key)).intValue();
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            return ((Number) key.getClass().getMethod("value").invoke(key)).intValue();
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            var field = key.getClass().getDeclaredField("value");
            field.setAccessible(true);
            return ((Number) field.get(key)).intValue();
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }

    private static int defaultKeyCodeForIntent(String intent) {
        return switch (intent) {
            case "inventory" -> GLFW.GLFW_KEY_E;
            case "chat" -> GLFW.GLFW_KEY_T;
            case "pause_menu" -> GLFW.GLFW_KEY_ESCAPE;
            default -> -1;
        };
    }

    private InputCommand keyPressIntent(int keyCode) {
        return new InputCommand(
                "key_press",
                0.0d,
                0.0d,
                0,
                keyCode,
                0,
                0,
                null,
                0
        );
    }

    private Number numberArgument(Map<String, Object> arguments, String key) {
        var value = arguments.get(key);
        if (value instanceof Number number) {
            return number;
        }
        throw new IllegalArgumentException("missing numeric argument: " + key);
    }

    private int intArgument(Map<String, Object> arguments, String key, int defaultValue) {
        var value = arguments.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("invalid integer argument: " + key);
    }

    private String stringArgument(Map<String, Object> arguments, String key, String defaultValue) {
        var value = arguments.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private void rememberPointer(ClientScreenMetrics metrics, InputCommand command) {
        if (!tracksPointer(command.action())) {
            return;
        }
        pointerStates.update(
                metrics.screenClass(),
                "minecraft",
                (int) Math.round(command.x()),
                (int) Math.round(command.y())
        );
    }

    private boolean tracksPointer(String action) {
        return "click".equals(action) || "move".equals(action) || "hover".equals(action);
    }

    private record ResolvedPoint(double x, double y) {
    }
}
