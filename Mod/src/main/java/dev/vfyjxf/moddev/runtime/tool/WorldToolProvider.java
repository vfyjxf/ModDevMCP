package dev.vfyjxf.moddev.runtime.tool;

import dev.vfyjxf.moddev.runtime.world.WorldCreateRequest;
import dev.vfyjxf.moddev.runtime.world.WorldJoinRequest;
import dev.vfyjxf.moddev.runtime.world.WorldService;
import dev.vfyjxf.moddev.runtime.world.WorldServiceException;
import dev.vfyjxf.moddev.server.api.McpToolDefinition;
import dev.vfyjxf.moddev.server.api.McpToolProvider;
import dev.vfyjxf.moddev.server.api.ToolCallContext;
import dev.vfyjxf.moddev.server.api.ToolResult;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WorldToolProvider implements McpToolProvider {

    private final WorldService worldService;

    public WorldToolProvider(WorldService worldService) {
        this.worldService = Objects.requireNonNull(worldService, "worldService");
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(listDefinition(), (context, arguments) -> invoke(() -> listPayload(context)));
        registry.registerTool(createDefinition(), (context, arguments) -> invoke(() -> createPayload(context, arguments)));
        registry.registerTool(joinDefinition(), (context, arguments) -> invoke(() -> joinPayload(context, arguments)));
    }

    private ToolResult invoke(java.util.function.Supplier<Map<String, Object>> action) {
        try {
            return ToolResult.success(action.get());
        } catch (WorldServiceException exception) {
            return ToolResult.failure(exception.errorCode());
        }
    }

    private Map<String, Object> listPayload(ToolCallContext context) {
        var result = worldService.listWorlds();
        var payload = runtimePayload(context);
        payload.put("worlds", result.worlds().stream().map(world -> Map.of(
                "id", world.id(),
                "name", world.name(),
                "lastPlayed", world.lastPlayed(),
                "gameMode", world.gameMode(),
                "hardcore", world.hardcore(),
                "cheatsKnown", world.cheatsKnown()
        )).toList());
        return Map.copyOf(payload);
    }

    private Map<String, Object> createPayload(ToolCallContext context, Map<String, Object> arguments) {
        var request = new WorldCreateRequest(
                stringArg(arguments, "name"),
                defaultedString(arguments, "gameMode", "survival"),
                booleanArg(arguments, "allowCheats", false),
                stringArg(arguments, "seed"),
                defaultedString(arguments, "worldType", "default"),
                defaultedString(arguments, "difficulty", "normal"),
                booleanArg(arguments, "bonusChest", false),
                booleanArg(arguments, "generateStructures", true),
                booleanArg(arguments, "joinAfterCreate", true)
        );
        var result = worldService.createWorld(request);
        var payload = runtimePayload(context);
        payload.put("worldId", result.worldId());
        payload.put("worldName", result.worldName());
        payload.put("created", result.created());
        payload.put("joined", result.joined());
        return Map.copyOf(payload);
    }

    private Map<String, Object> joinPayload(ToolCallContext context, Map<String, Object> arguments) {
        var request = new WorldJoinRequest(stringArg(arguments, "id"), stringArg(arguments, "name"));
        var result = worldService.joinWorld(request);
        var payload = runtimePayload(context);
        payload.put("worldId", result.worldId());
        payload.put("worldName", result.worldName());
        payload.put("joined", result.joined());
        return Map.copyOf(payload);
    }

    private McpToolDefinition listDefinition() {
        return new McpToolDefinition(
                "moddev.world_list",
                "World List",
                "Lists local worlds available to the connected client runtime.",
                Map.of(
                        "type", "object",
                        "properties", Map.of()
                ),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "runtimeId", Map.of("type", "string"),
                                "runtimeSide", Map.of("type", "string"),
                                "worlds", Map.of("type", "array")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "worlds")
                ),
                List.of("world", "list"),
                "client",
                false,
                false,
                "public",
                "public"
        );
    }

    private McpToolDefinition createDefinition() {
        return new McpToolDefinition(
                "moddev.world_create",
                "World Create",
                "Creates a local world and optionally joins it in the connected client runtime.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string", "description", "Local world display name."),
                                "gameMode", Map.of("type", "string", "enum", List.of("survival", "creative", "hardcore")),
                                "allowCheats", Map.of("type", "boolean"),
                                "seed", Map.of("type", "string"),
                                "worldType", Map.of(
                                        "type", "string",
                                        "enum", List.of("default", "flat", "large_biomes", "amplified"),
                                        "description", "World preset type. Use flat for superflat worlds."
                                ),
                                "difficulty", Map.of(
                                        "type", "string",
                                        "enum", List.of("peaceful", "easy", "normal", "hard"),
                                        "description", "Initial world difficulty."
                                ),
                                "bonusChest", Map.of("type", "boolean", "description", "Whether the world starts with a bonus chest."),
                                "generateStructures", Map.of("type", "boolean", "description", "Whether villages and other generated structures are enabled."),
                                "joinAfterCreate", Map.of("type", "boolean")
                        ),
                        "required", List.of("name")
                ),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "runtimeId", Map.of("type", "string"),
                                "runtimeSide", Map.of("type", "string"),
                                "worldId", Map.of("type", "string"),
                                "worldName", Map.of("type", "string"),
                                "created", Map.of("type", "boolean"),
                                "joined", Map.of("type", "boolean")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "worldId", "worldName", "created", "joined")
                ),
                List.of("world", "create"),
                "client",
                true,
                false,
                "public",
                "public"
        );
    }

    private McpToolDefinition joinDefinition() {
        return new McpToolDefinition(
                "moddev.world_join",
                "World Join",
                "Joins an existing local world in the connected client runtime.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "id", Map.of("type", "string", "description", "Stable world id returned by moddev.world_list."),
                                "name", Map.of("type", "string", "description", "Fallback world name when id is unknown.")
                        )
                ),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "runtimeId", Map.of("type", "string"),
                                "runtimeSide", Map.of("type", "string"),
                                "worldId", Map.of("type", "string"),
                                "worldName", Map.of("type", "string"),
                                "joined", Map.of("type", "boolean")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "worldId", "worldName", "joined")
                ),
                List.of("world", "join"),
                "client",
                true,
                false,
                "public",
                "public"
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

    private String defaultedString(Map<String, Object> arguments, String key, String defaultValue) {
        var value = stringArg(arguments, key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private boolean booleanArg(Map<String, Object> arguments, String key, boolean defaultValue) {
        var value = arguments.get(key);
        return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
    }
}

