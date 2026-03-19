package dev.vfyjxf.moddev.runtime.tool;

import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.server.api.McpToolDefinition;
import dev.vfyjxf.moddev.server.api.McpToolProvider;
import dev.vfyjxf.moddev.server.api.ToolResult;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;

import java.util.List;
import java.util.Map;

public final class InputToolProvider implements McpToolProvider {

    private final InputActionDispatcher dispatcher;

    public InputToolProvider(RuntimeRegistries registries) {
        this.dispatcher = new InputActionDispatcher(registries);
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
                                "properties", Map.of(
                                        "action", Map.of("type", "string"),
                                        "performed", Map.of("type", "boolean"),
                                        "controller", Map.of("type", "string")
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
                    return ToolResult.success(result.payload(actionName));
                }
        );
    }
}

