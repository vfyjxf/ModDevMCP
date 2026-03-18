package dev.vfyjxf.mcp.service.runtime;

import dev.vfyjxf.mcp.service.operation.OperationDefinition;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StatusOperationHandlers {

    private StatusOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(
            RuntimeOperationBindings.ToolOperationInvoker toolInvoker,
            RuntimeOperationBindings.StatusSnapshotProvider statusSnapshotProvider
    ) {
        return List.of(
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "status.get",
                                "status",
                                "Get Status",
                                "Returns current service readiness, game readiness, connected sides, and export location.",
                                false,
                                Set.of(),
                                RuntimeOperationBindings.objectSchema(Map.of(), List.of()),
                                Map.of("operationId", "status.get", "input", Map.of())
                        ),
                        (input, resolvedTargetSide) -> {
                            var snapshot = statusSnapshotProvider.snapshot();
                            var payload = new LinkedHashMap<String, Object>();
                            payload.put("serviceReady", snapshot.serviceReady());
                            payload.put("gameReady", snapshot.gameReady());
                            payload.put("connectedSides", snapshot.connectedSides());
                            payload.put("entrySkillId", snapshot.entrySkillId());
                            payload.put("exportRoot", snapshot.exportRoot().toAbsolutePath().normalize().toString());
                            payload.put("lastError", snapshot.lastError());
                            return Collections.unmodifiableMap(payload);
                        }
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "status.live_screen",
                                "status",
                                "Get Live Screen",
                                "Returns the active client screen summary when a client runtime is available.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(Map.of(), List.of()),
                                Map.of("operationId", "status.live_screen", "input", Map.of())
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.ui_get_live_screen")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "status.pause_on_lost_focus",
                                "status",
                                "Pause On Lost Focus",
                                "Queries or updates the pause-on-lost-focus client option.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of("enabled", Map.of("type", "boolean")),
                                        List.of()
                                ),
                                Map.of("operationId", "status.pause_on_lost_focus", "targetSide", "client", "input", Map.of())
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.pause_on_lost_focus")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "status.game_close",
                                "status",
                                "Close Game",
                                "Requests the connected runtime to shut down gracefully.",
                                true,
                                Set.of("client", "server"),
                                RuntimeOperationBindings.objectSchema(Map.of(), List.of()),
                                Map.of("operationId", "status.game_close", "targetSide", "client", "input", Map.of())
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.game_close")
                )
        );
    }
}
