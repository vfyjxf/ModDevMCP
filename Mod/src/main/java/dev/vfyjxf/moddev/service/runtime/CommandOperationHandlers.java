package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CommandOperationHandlers {

    private CommandOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeOperationBindings.ToolOperationInvoker toolInvoker) {
        return List.of(
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "command.list",
                                "command",
                                "List Commands",
                                "Lists available Minecraft commands for the selected runtime.",
                                true,
                                Set.of("client", "server"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of(
                                                "query", Map.of("type", "string"),
                                                "limit", Map.of("type", "integer")
                                        ),
                                        List.of()
                                ),
                                Map.of("operationId", "command.list", "targetSide", "server", "input", Map.of())
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.command_list")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "command.suggest",
                                "command",
                                "Suggest Command",
                                "Returns Brigadier suggestions for a partial command.",
                                true,
                                Set.of("client", "server"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of(
                                                "input", Map.of("type", "string"),
                                                "cursor", Map.of("type", "integer"),
                                                "limit", Map.of("type", "integer")
                                        ),
                                        List.of("input")
                                ),
                                Map.of(
                                        "operationId", "command.suggest",
                                        "targetSide", "server",
                                        "input", Map.of("input", "/sa")
                                )
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.command_suggest")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "command.execute",
                                "command",
                                "Execute Command",
                                "Executes a Minecraft command against the selected runtime.",
                                true,
                                Set.of("client", "server"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of("command", Map.of("type", "string")),
                                        List.of("command")
                                ),
                                Map.of(
                                        "operationId", "command.execute",
                                        "targetSide", "server",
                                        "input", Map.of("command", "/say hi")
                                )
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.command_execute")
                )
        );
    }
}

