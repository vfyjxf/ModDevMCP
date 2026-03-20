package dev.vfyjxf.mcp.service.runtime;

import dev.vfyjxf.mcp.service.operation.OperationDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UiOperationHandlers {

    private UiOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeOperationBindings.ToolOperationInvoker toolInvoker) {
        return List.of(
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "ui.inspect",
                                "ui",
                                "Inspect UI",
                                "Inspects the active screen and returns structured UI targets.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(Map.of(), List.of()),
                                Map.of("operationId", "ui.inspect", "targetSide", "client", "input", Map.of())
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.ui_inspect")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "ui.snapshot",
                                "ui",
                                "Snapshot UI",
                                "Captures the current UI snapshot and snapshot reference.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(Map.of(), List.of()),
                                Map.of("operationId", "ui.snapshot", "targetSide", "client", "input", Map.of())
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.ui_snapshot")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "ui.action",
                                "ui",
                                "Run UI Action",
                                "Performs a UI action against a selected target.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of(
                                                "action", Map.of("type", "string"),
                                                "target", Map.of("type", "object")
                                        ),
                                        List.of("action")
                                ),
                                Map.of(
                                        "operationId", "ui.action",
                                        "targetSide", "client",
                                        "input", Map.of("action", "click")
                                )
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.ui_action")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "ui.screenshot",
                                "ui",
                                "Screenshot UI",
                                "Captures a screenshot of the current UI or a selected target.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of(
                                                "sessionId", Map.of("type", "string"),
                                                "refId", Map.of("type", "string"),
                                                "ref", Map.of("type", "string"),
                                                "locator", Map.of("type", "object"),
                                                "x", Map.of("type", "integer"),
                                                "y", Map.of("type", "integer"),
                                                "mode", Map.of("type", "string"),
                                                "source", Map.of("type", "string"),
                                                "screenClass", Map.of("type", "string"),
                                                "modId", Map.of("type", "string")
                                        ),
                                        List.of()
                                ),
                                Map.of(
                                        "operationId", "ui.screenshot",
                                        "targetSide", "client",
                                        "input", Map.of("source", "auto")
                                )
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.ui_screenshot")
                )
        );
    }
}
