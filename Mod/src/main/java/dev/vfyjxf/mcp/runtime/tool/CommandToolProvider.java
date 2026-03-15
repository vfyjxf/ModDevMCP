package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.command.CommandExecutionRequest;
import dev.vfyjxf.mcp.runtime.command.CommandQuery;
import dev.vfyjxf.mcp.runtime.command.CommandService;
import dev.vfyjxf.mcp.runtime.command.CommandServiceException;
import dev.vfyjxf.mcp.runtime.command.CommandSuggestionQuery;
import dev.vfyjxf.mcp.runtime.command.CommandType;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CommandToolProvider implements McpToolProvider {

    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final int MAX_LIST_LIMIT = 200;
    private static final int DEFAULT_SUGGEST_LIMIT = 20;
    private static final int MAX_SUGGEST_LIMIT = 100;

    private final CommandService clientCommands;
    private final CommandService serverCommands;

    public CommandToolProvider(CommandService clientCommands, CommandService serverCommands) {
        this.clientCommands = clientCommands;
        this.serverCommands = serverCommands;
        if (clientCommands == null && serverCommands == null) {
            throw new IllegalArgumentException("At least one command service must be provided");
        }
    }

    public static CommandToolProvider clientAndServer(CommandService clientCommands, CommandService serverCommands) {
        return new CommandToolProvider(
                Objects.requireNonNull(clientCommands, "clientCommands"),
                Objects.requireNonNull(serverCommands, "serverCommands")
        );
    }

    public static CommandToolProvider serverOnly(CommandService serverCommands) {
        return new CommandToolProvider(null, Objects.requireNonNull(serverCommands, "serverCommands"));
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
        var commandSide = resolveCommandSide(context, arguments);
        var result = commandService(commandSide).list(new CommandQuery(stringArg(arguments, "query"), boundedInt(arguments, "limit", DEFAULT_LIST_LIMIT, MAX_LIST_LIMIT)));
        var payload = runtimePayload(context);
        payload.put("commandSide", commandSide.side());
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
        var commandSide = resolveCommandSide(context, arguments);
        var result = commandService(commandSide).suggest(new CommandSuggestionQuery(
                normalizedInput,
                cursor,
                boundedInt(arguments, "limit", DEFAULT_SUGGEST_LIMIT, MAX_SUGGEST_LIMIT)
        ));
        var payload = runtimePayload(context);
        payload.put("commandSide", commandSide.side());
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
        var commandSide = resolveCommandSide(context, arguments);
        var result = commandService(commandSide).execute(new CommandExecutionRequest(normalizeCommand(stringArg(arguments, "command"))));
        var payload = runtimePayload(context);
        payload.put("commandSide", commandSide.side());
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
                "Lists available Minecraft commands for the selected runtime. Use commandSide to choose the command context inside that runtime.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "commandSide", commandSideSchema(),
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
                                "commandSide", Map.of("type", "string"),
                                "commands", Map.of("type", "array"),
                                "truncated", Map.of("type", "boolean"),
                                "totalMatched", Map.of("type", "integer")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "commandSide", "commands", "truncated", "totalMatched")
                ),
                List.of("command", "discover"),
                "common",
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
                "Returns Brigadier suggestions for a partial Minecraft command. Use commandSide to choose the dispatcher inside the selected runtime.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "commandSide", commandSideSchema(),
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
                                "commandSide", Map.of("type", "string"),
                                "normalizedInput", Map.of("type", "string"),
                                "parseValidUpTo", Map.of("type", "integer"),
                                "suggestions", Map.of("type", "array")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "commandSide", "normalizedInput", "parseValidUpTo", "suggestions")
                ),
                List.of("command", "suggest"),
                "common",
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
                "Executes a Minecraft command against the selected available runtime. Use commandSide to choose the command context inside that runtime.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "commandSide", commandSideSchema(),
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
                                "commandSide", Map.of("type", "string"),
                                "normalizedCommand", Map.of("type", "string"),
                                "executed", Map.of("type", "boolean"),
                                "resultCode", Map.of("type", "integer"),
                                "messages", Map.of("type", "array"),
                                "errorCode", Map.of("type", "string"),
                                "errorDetail", Map.of("type", "string")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "commandSide", "normalizedCommand", "executed", "messages")
                ),
                List.of("command", "execute"),
                "common",
                false,
                false,
                "public",
                "public"
        );
    }

    private Map<String, Object> commandSideSchema() {
        return Map.of(
                "type", "string",
                "enum", List.of("client", "server"),
                "description", "Which command context to use inside the selected runtime. Use client for NeoForge client commands and server for server commands inside that runtime, including integrated server commands when running in client-runtime."
        );
    }

    private Map<String, Object> runtimePayload(ToolCallContext context) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("runtimeId", context.metadata().get("runtimeId") instanceof String value ? value : "");
        payload.put("runtimeSide", context.side() == null ? "" : context.side());
        return payload;
    }

    private CommandType resolveCommandSide(ToolCallContext context, Map<String, Object> arguments) {
        var requested = Optional.ofNullable(stringArg(arguments, "commandSide"))
                .or(() -> Optional.ofNullable(stringArg(arguments, "targetSide")))
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse("");
        if ("client".equals(requested)) {
            return CommandType.CLIENT;
        }
        if ("server".equals(requested)) {
            return CommandType.SERVER;
        }
        return switch (context.side() == null ? "" : context.side().toLowerCase()) {
            case "client" -> clientCommands != null ? CommandType.CLIENT : CommandType.SERVER;
            case "server" -> CommandType.SERVER;
            default -> serverCommands != null ? CommandType.SERVER : CommandType.CLIENT;
        };
    }

    private CommandService commandService(CommandType targetSide) {
        return switch (targetSide) {
            case CLIENT -> {
                if (clientCommands == null) {
                    throw new CommandServiceException("command_runtime_unavailable", "Client commands are unavailable on this runtime");
                }
                yield clientCommands;
            }
            case SERVER -> {
                if (serverCommands == null) {
                    throw new CommandServiceException("command_runtime_unavailable", "Server commands are unavailable on this runtime");
                }
                yield serverCommands;
            }
        };
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
