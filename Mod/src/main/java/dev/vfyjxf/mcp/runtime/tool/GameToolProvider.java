package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.game.GameCloser;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GameToolProvider implements McpToolProvider {

    private final GameCloser gameCloser;

    public GameToolProvider(GameCloser gameCloser) {
        this.gameCloser = Objects.requireNonNull(gameCloser, "gameCloser");
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(
                new McpToolDefinition(
                        "moddev.game_close",
                        "Game Close",
                        "Requests the connected Minecraft game runtime to shut down gracefully",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "targetSide", Map.of(
                                                "type", "string",
                                                "enum", List.of("client", "server")
                                        )
                                ),
                                "required", List.of()
                        ),
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "accepted", Map.of("type", "boolean"),
                                        "runtimeId", Map.of("type", "string"),
                                        "runtimeSide", Map.of("type", "string")
                                ),
                                "required", List.of("accepted", "runtimeId", "runtimeSide")
                        ),
                        List.of("game", "lifecycle"),
                        "common",
                        false,
                        false,
                        "public",
                        "public"
                ),
                (context, arguments) -> {
                    if (!gameCloser.requestClose()) {
                        return ToolResult.failure("game_close_rejected");
                    }
                    return ToolResult.success(resultPayload(context));
                }
        );
    }

    private Map<String, Object> resultPayload(dev.vfyjxf.mcp.server.api.ToolCallContext context) {
        var runtimeId = context.metadata().get("runtimeId") instanceof String value ? value : "";
        var runtimeSide = context.side() == null || context.side().isBlank() ? "" : context.side();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("accepted", true);
        payload.put("runtimeId", runtimeId);
        payload.put("runtimeSide", runtimeSide);
        return Map.copyOf(payload);
    }
}
