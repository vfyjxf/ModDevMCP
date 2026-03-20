package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class StatusOperationHandlers {

    private StatusOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(
            RuntimeRegistries registries,
            RuntimeOperationBindings.StatusSnapshotProvider statusSnapshotProvider
    ) {
        Objects.requireNonNull(registries, "registries");
        Objects.requireNonNull(statusSnapshotProvider, "statusSnapshotProvider");
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
                            payload.put("usageSkillId", snapshot.usageSkillId());
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
                        (input, resolvedTargetSide) -> {
                            var probe = registries.screenProbe("client")
                                    .orElseThrow(() -> RuntimeOperationBindings.executionFailure("client screen probe unavailable"));
                            var metrics = probe.metrics();
                            var active = metrics.screenClass() != null && !metrics.screenClass().isBlank();
                            return Map.of(
                                    "active", active,
                                    "screenClass", active ? metrics.screenClass() : "",
                                    "modId", active ? "minecraft" : "",
                                    "guiWidth", metrics.guiWidth(),
                                    "guiHeight", metrics.guiHeight(),
                                    "framebufferWidth", metrics.framebufferWidth(),
                                    "framebufferHeight", metrics.framebufferHeight()
                            );
                        }
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
                        (input, resolvedTargetSide) -> {
                            var service = registries.pauseOnLostFocusService("client")
                                    .orElseThrow(() -> RuntimeOperationBindings.executionFailure("pause-on-lost-focus runtime unavailable"));
                            var enabledArg = input.get("enabled");
                            var changed = false;
                            final boolean enabled;
                            if (enabledArg instanceof Boolean enabledValue) {
                                changed = service.setEnabled(enabledValue);
                                enabled = enabledValue;
                            } else {
                                enabled = service.currentState();
                            }
                            return Map.of(
                                    "runtimeSide", "client",
                                    "enabled", enabled,
                                    "changed", changed
                            );
                        }
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
                        (input, resolvedTargetSide) -> {
                            var side = resolvedTargetSide == null || resolvedTargetSide.isBlank() ? "client" : resolvedTargetSide;
                            var gameCloser = registries.gameCloser(side)
                                    .orElseThrow(() -> RuntimeOperationBindings.executionFailure("game closer unavailable for side " + side));
                            if (!gameCloser.requestClose()) {
                                throw RuntimeOperationBindings.executionFailure("game_close_rejected");
                            }
                            return Map.of(
                                    "accepted", true,
                                    "runtimeSide", side
                            );
                        }
                )
        );
    }
}
