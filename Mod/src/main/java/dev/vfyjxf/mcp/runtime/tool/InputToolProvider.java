package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.api.runtime.ClientScreenMetrics;
import dev.vfyjxf.mcp.api.runtime.ClientScreenProbe;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.List;
import java.util.Map;

public final class InputToolProvider implements McpToolProvider {

    @FunctionalInterface
    interface ClipboardSetter {
        void set(String text);
    }

    private final InputActionDispatcher dispatcher;
    private final ClientScreenProbe screenProbe;
    private final ClipboardSetter clipboardSetter;

    public InputToolProvider(RuntimeRegistries registries) {
        this(registries, InputToolProvider::liveClientMetrics, InputToolProvider::setLiveClipboard);
    }

    InputToolProvider(RuntimeRegistries registries, ClientScreenProbe screenProbe) {
        this(registries, screenProbe, InputToolProvider::setLiveClipboard);
    }

    InputToolProvider(RuntimeRegistries registries, ClientScreenProbe screenProbe, ClipboardSetter clipboardSetter) {
        this.dispatcher = new InputActionDispatcher(registries);
        this.screenProbe = screenProbe;
        this.clipboardSetter = clipboardSetter;
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(
                new McpToolDefinition(
                        "moddev.input_action",
                        "moddev.input_action",
                        "Built-in input tool",
                        Map.of(
                                "type", "object",
                                "properties", Map.ofEntries(
                                        Map.entry("action", Map.of(
                                                "type", "string",
                                                "enum", List.of(
                                                        "click",
                                                        "move",
                                                        "hover",
                                                        "mouse_down",
                                                        "mouse_up",
                                                        "key_press",
                                                        "key_down",
                                                        "key_up",
                                                        "key_click",
                                                        "type_text",
                                                        "ui_intent"
                                                )
                                        )),
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
                                "required", List.of("action")
                        ),
                        Map.of(
                                "type", "object",
                                "properties", Map.ofEntries(
                                        Map.entry("action", Map.of("type", "string")),
                                        Map.entry("performed", Map.of("type", "boolean")),
                                        Map.entry("controller", Map.of("type", "string")),
                                        Map.entry("screenClass", Map.of("type", "string")),
                                        Map.entry("modId", Map.of("type", "string")),
                                        Map.entry("screenAvailable", Map.of("type", "boolean")),
                                        Map.entry("inWorld", Map.of("type", "boolean")),
                                        Map.entry("guiWidth", Map.of("type", "integer")),
                                        Map.entry("guiHeight", Map.of("type", "integer")),
                                        Map.entry("framebufferWidth", Map.of("type", "integer")),
                                        Map.entry("framebufferHeight", Map.of("type", "integer"))
                                ),
                                "required", List.of("action", "performed", "controller")
                        ),
                        List.of("input"),
                        "client",
                        false,
                        false,
                        "public",
                        "public"
                ),
                (context, arguments) -> {
                    var action = arguments.get("action");
                    if (!(action instanceof String actionName) || actionName.isBlank()) {
                        return ToolResult.failure("invalid_input: missing action");
                    }
                    var result = dispatcher.dispatch(actionName, Map.copyOf(arguments));
                    if (!result.success()) {
                        return ToolResult.failure(result.error());
                    }
                    return ToolResult.success(withScreenState(result.payload(actionName), liveMetrics()));
                }
        );
        registry.registerTool(
                new McpToolDefinition(
                        "moddev.input_clipboard_set",
                        "moddev.input_clipboard_set",
                        "Built-in clipboard tool",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "text", Map.of("type", "string")
                                ),
                                "required", List.of("text")
                        ),
                        Map.of(
                                "type", "object",
                                "properties", Map.ofEntries(
                                        Map.entry("performed", Map.of("type", "boolean")),
                                        Map.entry("controller", Map.of("type", "string")),
                                        Map.entry("textLength", Map.of("type", "integer")),
                                        Map.entry("screenClass", Map.of("type", "string")),
                                        Map.entry("modId", Map.of("type", "string")),
                                        Map.entry("screenAvailable", Map.of("type", "boolean")),
                                        Map.entry("inWorld", Map.of("type", "boolean")),
                                        Map.entry("guiWidth", Map.of("type", "integer")),
                                        Map.entry("guiHeight", Map.of("type", "integer")),
                                        Map.entry("framebufferWidth", Map.of("type", "integer")),
                                        Map.entry("framebufferHeight", Map.of("type", "integer"))
                                ),
                                "required", List.of("performed", "controller", "textLength")
                        ),
                        List.of("input"),
                        "client",
                        false,
                        false,
                        "public",
                        "public"
                ),
                (context, arguments) -> {
                    var text = arguments.get("text");
                    if (!(text instanceof String stringText)) {
                        return ToolResult.failure("invalid_input: missing text");
                    }
                    try {
                        clipboardSetter.set(stringText);
                    } catch (RuntimeException exception) {
                        return ToolResult.failure(exception.getMessage() == null || exception.getMessage().isBlank()
                                ? "clipboard_unavailable"
                                : exception.getMessage());
                    }
                    return ToolResult.success(withScreenState(Map.of(
                            "performed", true,
                            "controller", "minecraft-live-client",
                            "textLength", stringText.length()
                    ), liveMetrics()));
                }
        );
    }

    /**
     * Input tools should always report whether they ran against a real screen or directly against
     * the in-world client so callers can distinguish raw game input from screen-bound UI input.
     */
    private Map<String, Object> withScreenState(Map<String, Object> payload, ClientScreenMetrics metrics) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.putAll(payload);
        result.put("screenClass", screenClass(metrics));
        result.put("modId", "minecraft");
        result.put("screenAvailable", hasScreen(metrics));
        result.put("inWorld", !hasScreen(metrics));
        result.put("guiWidth", metrics.guiWidth());
        result.put("guiHeight", metrics.guiHeight());
        result.put("framebufferWidth", metrics.framebufferWidth());
        result.put("framebufferHeight", metrics.framebufferHeight());
        return Map.copyOf(result);
    }

    private ClientScreenMetrics liveMetrics() {
        try {
            return screenProbe.metrics();
        } catch (RuntimeException exception) {
            return new ClientScreenMetrics(null, 0, 0, 0, 0);
        }
    }

    private static ClientScreenMetrics liveClientMetrics() {
        try {
            var minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            var minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return new ClientScreenMetrics(null, 0, 0, 0, 0);
            }
            var screen = minecraftClass.getField("screen").get(minecraft);
            var window = minecraftClass.getMethod("getWindow").invoke(minecraft);
            var windowClass = window.getClass();
            return new ClientScreenMetrics(
                    screen == null ? null : screen.getClass().getName(),
                    ((Number) windowClass.getMethod("getGuiScaledWidth").invoke(window)).intValue(),
                    ((Number) windowClass.getMethod("getGuiScaledHeight").invoke(window)).intValue(),
                    ((Number) windowClass.getMethod("getWidth").invoke(window)).intValue(),
                    ((Number) windowClass.getMethod("getHeight").invoke(window)).intValue()
            );
        } catch (ReflectiveOperationException | LinkageError exception) {
            return new ClientScreenMetrics(null, 0, 0, 0, 0);
        }
    }

    /**
     * Clipboard updates stay inside the live Minecraft client so paste flows can be verified
     * through ModDevMCP without falling back to OS-level automation.
     */
    private static void setLiveClipboard(String text) {
        try {
            var minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            var minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                throw new IllegalStateException("clipboard_unavailable");
            }
            var keyboardHandlerField = minecraftClass.getField("keyboardHandler");
            var keyboardHandler = keyboardHandlerField.get(minecraft);
            if (keyboardHandler == null) {
                throw new IllegalStateException("clipboard_unavailable");
            }
            keyboardHandler.getClass()
                    .getMethod("setClipboard", String.class)
                    .invoke(keyboardHandler, text == null ? "" : text);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("clipboard_unavailable", exception);
        }
    }

    private boolean hasScreen(ClientScreenMetrics metrics) {
        return metrics.screenClass() != null && !metrics.screenClass().isBlank();
    }

    private String screenClass(ClientScreenMetrics metrics) {
        return hasScreen(metrics) ? metrics.screenClass() : "";
    }
}
