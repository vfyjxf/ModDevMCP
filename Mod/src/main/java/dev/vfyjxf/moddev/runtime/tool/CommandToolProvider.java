package dev.vfyjxf.moddev.runtime.tool;

import dev.vfyjxf.moddev.runtime.command.CommandExecutionRequest;
import dev.vfyjxf.moddev.runtime.command.CommandQuery;
import dev.vfyjxf.moddev.runtime.command.CommandService;
import dev.vfyjxf.moddev.runtime.command.CommandServiceException;
import dev.vfyjxf.moddev.runtime.command.CommandSuggestionQuery;
import dev.vfyjxf.moddev.server.api.McpToolDefinition;
import dev.vfyjxf.moddev.server.api.McpToolProvider;
import dev.vfyjxf.moddev.server.api.ToolCallContext;
import dev.vfyjxf.moddev.server.api.ToolResult;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CommandToolProvider implements McpToolProvider {

    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final int MAX_LIST_LIMIT = 200;
    private static final int DEFAULT_SUGGEST_LIMIT = 20;
    private static final int MAX_SUGGEST_LIMIT = 100;

    private final CommandService commandService;
    private final String runtimeSide;

    private CommandToolProvider(CommandService commandService, String runtimeSide) {
        this.commandService = Objects.requireNonNull(commandService, "commandService");
        this.runtimeSide = Objects.requireNonNull(runtimeSide, "runtimeSide");
    }

    public static CommandToolProvider clientOnly(CommandService clientCommands) {
        return new CommandToolProvider(Objects.requireNonNull(clientCommands, "clientCommands"), "client");
    }

    public static CommandToolProvider serverOnly(CommandService serverCommands) {
        return new CommandToolProvider(Objects.requireNonNull(serverCommands, "serverCommands"), "server");
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(listDefinition(), (context, arguments) -> invoke(() -> listPayload(context, arguments)));
        registry.registerTool(suggestDefinition(), (context, arguments) -> invoke(() -> suggestPayload(context, arguments)));
        registry.registerTool(executeDefinition(), (context, arguments) -> invoke(() -> executePayload(context, arguments)));
    }

    private ToolResult invoke(java.util.function.Supplier<Map<String, Object>> action) {
        try {
            return ToolResult.success(action.get());
        } catch (CommandServiceException exception) {
            return ToolResult.failure(exception.errorCode());
        }
    }

    private Map<String, Object> listPayload(ToolCallContext context, Map<String, Object> arguments) {
        var result = commandService.list(new CommandQuery(stringArg(arguments, "query"), boundedInt(arguments, "limit", DEFAULT_LIST_LIMIT, MAX_LIST_LIMIT)));
        var payload = runtimePayload(context);
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
    }

    private Map<String, Object> suggestPayload(ToolCallContext context, Map<String, Object> arguments) {
        var input = stringArg(arguments, "input");
        var normalizedInput = input == null ? "" : input;
        var cursor = arguments.get("cursor") instanceof Number number ? number.intValue() : normalizedInput.length();
        var result = commandService.suggest(new CommandSuggestionQuery(
                normalizedInput,
                cursor,
                boundedInt(arguments, "limit", DEFAULT_SUGGEST_LIMIT, MAX_SUGGEST_LIMIT)
        ));
        var payload = runtimePayload(context);
        payload.put("normalizedInput", result.normalizedInput());
        payload.put("parseValidUpTo", result.parseValidUpTo());
        payload.put("suggestions", result.suggestions().stream().map(suggestion -> Map.of(
                "text", suggestion.text(),
                "rangeStart", suggestion.rangeStart(),
                "rangeEnd", suggestion.rangeEnd(),
                "tooltip", suggestion.tooltip()
        )).toList());
        return Map.copyOf(payload);
    }

    private Map<String, Object> executePayload(ToolCallContext context, Map<String, Object> arguments) {
        var result = commandService.execute(new CommandExecutionRequest(normalizeCommand(stringArg(arguments, "command"))));
        var payload = runtimePayload(context);
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
    }

    private McpToolDefinition listDefinition() {
        return new McpToolDefinition(
                "moddev.command_list",
                "Command List",
                "Lists available Minecraft commands for the selected runtime.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "targetSide", targetSideSchema(),
                                "query", Map.of(
                                        "type", "string",
                                        "description", "Optional filter applied to command names, usage text, and summaries."
                                ),
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Maximum number of commands to return."
                                )
                        )
                ),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "runtimeId", Map.of("type", "string"),
                                "runtimeSide", Map.of("type", "string"),
                                "commands", Map.of("type", "array"),
                                "truncated", Map.of("type", "boolean"),
                                "totalMatched", Map.of("type", "integer")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "commands", "truncated", "totalMatched")
                ),
                List.of("command", "discover"),
                runtimeSide,
                false,
                false,
                "public",
                "public"
        );
    }

    private McpToolDefinition suggestDefinition() {
        return new McpToolDefinition(
                "moddev.command_suggest",
                "Command Suggest",
                "Returns Brigadier suggestions for a partial Minecraft command against the selected runtime.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "targetSide", targetSideSchema(),
                                "input", Map.of(
                                        "type", "string",
                                        "description", "Partial command input, with or without a leading slash."
                                ),
                                "cursor", Map.of(
                                        "type", "integer",
                                        "description", "Cursor position inside input. Defaults to the end of the string."
                                ),
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Maximum number of suggestions to return."
                                )
                        ),
                        "required", List.of("input")
                ),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "runtimeId", Map.of("type", "string"),
                                "runtimeSide", Map.of("type", "string"),
                                "normalizedInput", Map.of("type", "string"),
                                "parseValidUpTo", Map.of("type", "integer"),
                                "suggestions", Map.of("type", "array")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "normalizedInput", "parseValidUpTo", "suggestions")
                ),
                List.of("command", "suggest"),
                runtimeSide,
                false,
                false,
                "public",
                "public"
        );
    }

    private McpToolDefinition executeDefinition() {
        return new McpToolDefinition(
                "moddev.command_execute",
                "Command Execute",
                "Executes a Minecraft command against the selected runtime.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "targetSide", targetSideSchema(),
                                "command", Map.of(
                                        "type", "string",
                                        "description", "Command text to execute, with or without a leading slash."
                                )
                        ),
                        "required", List.of("command")
                ),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "runtimeId", Map.of("type", "string"),
                                "runtimeSide", Map.of("type", "string"),
                                "normalizedCommand", Map.of("type", "string"),
                                "executed", Map.of("type", "boolean"),
                                "resultCode", Map.of("type", "integer"),
                                "messages", Map.of("type", "array"),
                                "errorCode", Map.of("type", "string"),
                                "errorDetail", Map.of("type", "string")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "normalizedCommand", "executed", "messages")
                ),
                List.of("command", "execute"),
                runtimeSide,
                false,
                false,
                "public",
                "public"
        );
    }

    private Map<String, Object> targetSideSchema() {
        return Map.of(
                "type", "string",
                "enum", List.of("client", "server"),
                "description", "Which connected runtime should receive the command tool call when multiple runtimes are available."
        );
    }

    private Map<String, Object> runtimePayload(ToolCallContext context) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("runtimeId", context.metadata().get("runtimeId") instanceof String value ? value : "");
        payload.put("runtimeSide", context.side() == null ? "" : context.side());
        return payload;
    }

    private String stringArg(Map<String, Object> arguments, String key) {
        var value = arguments.get(key);
        return value instanceof String stringValue ? stringValue : null;
    }

    private int boundedInt(Map<String, Object> arguments, String key, int defaultValue, int maxValue) {
        var value = arguments.get(key);
        if (!(value instanceof Number number)) {
            return defaultValue;
        }
        return Math.max(1, Math.min(number.intValue(), maxValue));
    }

    private String normalizeCommand(String command) {
        if (command == null) {
            return "";
        }
        var normalized = command.trim();
        return normalized.startsWith("/") ? normalized.substring(1).trim() : normalized;
    }
}

