package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.runtime.command.CommandExecutionRequest;
import dev.vfyjxf.moddev.runtime.command.CommandQuery;
import dev.vfyjxf.moddev.runtime.command.CommandServiceException;
import dev.vfyjxf.moddev.runtime.command.CommandSuggestionQuery;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CommandOperationHandlers {

    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final int MAX_LIST_LIMIT = 200;
    private static final int DEFAULT_SUGGEST_LIMIT = 20;
    private static final int MAX_SUGGEST_LIMIT = 100;

    private CommandOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeRegistries registries) {
        Objects.requireNonNull(registries, "registries");
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
                        (input, resolvedTargetSide) -> {
                            var side = normalizedSide(resolvedTargetSide);
                            var service = registries.commandService(side)
                                    .orElseThrow(() -> RuntimeOperationBindings.executionFailure("command runtime unavailable for side " + side));
                            try {
                                var result = service.list(new CommandQuery(stringArg(input, "query"), boundedInt(input, "limit", DEFAULT_LIST_LIMIT, MAX_LIST_LIMIT)));
                                var payload = runtimePayload(side);
                                payload.put("commands", result.commands().stream().map(command -> Map.of(
                                        "name", command.name(),
                                        "usage", command.usage(),
                                        "source", command.source(),
                                        "side", command.type().side(),
                                        "namespace", command.namespace(),
                                        "summary", command.summary()
                                )).toList());
                                payload.put("truncated", result.truncated());
                                payload.put("totalMatched", result.totalMatched());
                                return Map.copyOf(payload);
                            } catch (CommandServiceException exception) {
                                throw RuntimeOperationBindings.executionFailure(exception.errorCode());
                            }
                        }
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
                        (input, resolvedTargetSide) -> {
                            var side = normalizedSide(resolvedTargetSide);
                            var service = registries.commandService(side)
                                    .orElseThrow(() -> RuntimeOperationBindings.executionFailure("command runtime unavailable for side " + side));
                            try {
                                var normalizedInput = stringArg(input, "input") == null ? "" : stringArg(input, "input");
                                var cursor = input.get("cursor") instanceof Number number ? number.intValue() : normalizedInput.length();
                                var result = service.suggest(new CommandSuggestionQuery(
                                        normalizedInput,
                                        cursor,
                                        boundedInt(input, "limit", DEFAULT_SUGGEST_LIMIT, MAX_SUGGEST_LIMIT)
                                ));
                                var payload = runtimePayload(side);
                                payload.put("normalizedInput", result.normalizedInput());
                                payload.put("parseValidUpTo", result.parseValidUpTo());
                                payload.put("suggestions", result.suggestions().stream().map(suggestion -> Map.of(
                                        "text", suggestion.text(),
                                        "rangeStart", suggestion.rangeStart(),
                                        "rangeEnd", suggestion.rangeEnd(),
                                        "tooltip", suggestion.tooltip()
                                )).toList());
                                return Map.copyOf(payload);
                            } catch (CommandServiceException exception) {
                                throw RuntimeOperationBindings.executionFailure(exception.errorCode());
                            }
                        }
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
                        (input, resolvedTargetSide) -> {
                            var side = normalizedSide(resolvedTargetSide);
                            var service = registries.commandService(side)
                                    .orElseThrow(() -> RuntimeOperationBindings.executionFailure("command runtime unavailable for side " + side));
                            try {
                                var result = service.execute(new CommandExecutionRequest(normalizeCommand(stringArg(input, "command"))));
                                var payload = runtimePayload(side);
                                payload.put("normalizedCommand", result.normalizedCommand());
                                payload.put("executed", result.executed());
                                payload.put("messages", result.messages());
                                if (result.resultCode() != null) {
                                    payload.put("resultCode", result.resultCode());
                                }
                                if (!result.errorCode().isBlank()) {
                                    payload.put("errorCode", result.errorCode());
                                }
                                if (!result.errorDetail().isBlank()) {
                                    payload.put("errorDetail", result.errorDetail());
                                }
                                return Map.copyOf(payload);
                            } catch (CommandServiceException exception) {
                                throw RuntimeOperationBindings.executionFailure(exception.errorCode());
                            }
                        }
                )
        );
    }

    private static Map<String, Object> runtimePayload(String side) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("runtimeSide", side);
        return payload;
    }

    private static String normalizedSide(String side) {
        return side == null || side.isBlank() ? "client" : side;
    }

    private static String stringArg(Map<String, Object> arguments, String key) {
        var value = arguments.get(key);
        return value instanceof String stringValue ? stringValue : null;
    }

    private static int boundedInt(Map<String, Object> arguments, String key, int defaultValue, int maxValue) {
        var value = arguments.get(key);
        if (!(value instanceof Number number)) {
            return defaultValue;
        }
        return Math.max(1, Math.min(number.intValue(), maxValue));
    }

    private static String normalizeCommand(String command) {
        if (command == null) {
            return "";
        }
        var normalized = command.trim();
        return normalized.startsWith("/") ? normalized.substring(1).trim() : normalized;
    }
}
