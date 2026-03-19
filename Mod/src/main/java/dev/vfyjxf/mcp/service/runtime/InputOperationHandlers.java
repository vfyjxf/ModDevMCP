package dev.vfyjxf.mcp.service.runtime;

import dev.vfyjxf.mcp.service.operation.OperationDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exposes the low-level client input bridge through the HTTP operation catalog so callers can
 * drive keyboard, mouse, and clipboard flows without leaving ModDevMCP.
 */
public final class InputOperationHandlers {

    private InputOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeOperationBindings.ToolOperationInvoker toolInvoker) {
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
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.input_action")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "input.clipboard_set",
                                "input",
                                "Set Clipboard",
                                "Updates the live client clipboard so later paste shortcuts stay inside ModDevMCP.",
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
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.input_clipboard_set")
                )
        );
    }
}
