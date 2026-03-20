package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.runtime.world.WorldCreateRequest;
import dev.vfyjxf.moddev.runtime.world.WorldJoinRequest;
import dev.vfyjxf.moddev.runtime.world.WorldServiceException;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WorldOperationHandlers {

    private WorldOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeRegistries registries) {
        Objects.requireNonNull(registries, "registries");
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
                        (input, resolvedTargetSide) -> {
                            var service = registries.worldService("client")
                                    .orElseThrow(() -> RuntimeOperationBindings.executionFailure("world runtime unavailable"));
                            try {
                                var result = service.listWorlds();
                                var payload = runtimePayload();
                                payload.put("worlds", result.worlds().stream().map(world -> Map.of(
                                        "id", world.id(),
                                        "name", world.name(),
                                        "lastPlayed", world.lastPlayed(),
                                        "gameMode", world.gameMode(),
                                        "hardcore", world.hardcore(),
                                        "cheatsKnown", world.cheatsKnown()
                                )).toList());
                                return Map.copyOf(payload);
                            } catch (WorldServiceException exception) {
                                throw worldFailure(exception);
                            }
                        }
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
                        (input, resolvedTargetSide) -> {
                            var service = registries.worldService("client")
                                    .orElseThrow(() -> RuntimeOperationBindings.executionFailure("world runtime unavailable"));
                            try {
                                var request = new WorldCreateRequest(
                                        stringArg(input, "name"),
                                        defaultedString(input, "gameMode", "survival"),
                                        booleanArg(input, "allowCheats", false),
                                        stringArg(input, "seed"),
                                        defaultedString(input, "worldType", "default"),
                                        defaultedString(input, "difficulty", "normal"),
                                        booleanArg(input, "bonusChest", false),
                                        booleanArg(input, "generateStructures", true),
                                        booleanArg(input, "joinAfterCreate", true)
                                );
                                var result = service.createWorld(request);
                                var payload = runtimePayload();
                                payload.put("worldId", result.worldId());
                                payload.put("worldName", result.worldName());
                                payload.put("created", result.created());
                                payload.put("joined", result.joined());
                                return Map.copyOf(payload);
                            } catch (WorldServiceException exception) {
                                throw worldFailure(exception);
                            }
                        }
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
                        (input, resolvedTargetSide) -> {
                            var service = registries.worldService("client")
                                    .orElseThrow(() -> RuntimeOperationBindings.executionFailure("world runtime unavailable"));
                            try {
                                var request = new WorldJoinRequest(stringArg(input, "id"), stringArg(input, "name"));
                                var result = service.joinWorld(request);
                                var payload = runtimePayload();
                                payload.put("worldId", result.worldId());
                                payload.put("worldName", result.worldName());
                                payload.put("joined", result.joined());
                                return Map.copyOf(payload);
                            } catch (WorldServiceException exception) {
                                throw worldFailure(exception);
                            }
                        }
                )
        );
    }

    private static Map<String, Object> runtimePayload() {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("runtimeSide", "client");
        return payload;
    }

    private static dev.vfyjxf.moddev.service.request.OperationExecutionException worldFailure(WorldServiceException exception) {
        return RuntimeOperationBindings.executionFailure(
                exception.errorCode() + ": " + RuntimeOperationBindings.normalizeMessage(exception.getMessage(), exception.errorCode())
        );
    }

    private static String stringArg(Map<String, Object> arguments, String key) {
        var value = arguments.get(key);
        return value instanceof String stringValue ? stringValue : null;
    }

    private static String defaultedString(Map<String, Object> arguments, String key, String defaultValue) {
        var value = stringArg(arguments, key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static boolean booleanArg(Map<String, Object> arguments, String key, boolean defaultValue) {
        var value = arguments.get(key);
        return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
    }
}

