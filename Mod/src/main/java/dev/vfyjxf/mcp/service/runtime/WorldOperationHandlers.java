package dev.vfyjxf.mcp.service.runtime;

import dev.vfyjxf.mcp.service.operation.OperationDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorldOperationHandlers {

    private WorldOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeOperationBindings.ToolOperationInvoker toolInvoker) {
        return List.of(
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "world.list",
                                "world",
                                "List Worlds",
                                "Lists local worlds available to the connected client runtime.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(Map.of(), List.of()),
                                Map.of("operationId", "world.list", "targetSide", "client", "input", Map.of())
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.world_list")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "world.create",
                                "world",
                                "Create World",
                                "Creates a local world and optionally joins it.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of(
                                                "name", Map.of("type", "string"),
                                                "gameMode", Map.of("type", "string"),
                                                "allowCheats", Map.of("type", "boolean")
                                        ),
                                        List.of("name")
                                ),
                                Map.of(
                                        "operationId", "world.create",
                                        "targetSide", "client",
                                        "input", Map.of("name", "Test World")
                                )
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.world_create")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "world.join",
                                "world",
                                "Join World",
                                "Joins an existing local world.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of(
                                                "id", Map.of("type", "string"),
                                                "name", Map.of("type", "string")
                                        ),
                                        List.of()
                                ),
                                Map.of(
                                        "operationId", "world.join",
                                        "targetSide", "client",
                                        "input", Map.of("name", "Test World")
                                )
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.world_join")
                )
        );
    }
}
