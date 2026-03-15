package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.game.PauseOnLostFocusService;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PauseOnLostFocusToolProvider implements McpToolProvider {

    private final PauseOnLostFocusService service;

    public PauseOnLostFocusToolProvider(PauseOnLostFocusService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(definition(), (context, arguments) -> {
            try {
                return ToolResult.success(payload(context, arguments));
            } catch (RuntimeException exception) {
                return ToolResult.failure("pause_on_lost_focus_unavailable");
            }
        });
    }

    private McpToolDefinition definition() {
        return new McpToolDefinition(
                "moddev.pause_on_lost_focus",
                "Pause On Lost Focus",
                "Queries or updates Minecraft's pause-on-lost-focus client option and persists the setting.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "enabled", Map.of(
                                        "type", "boolean",
                                        "description", "When provided, sets whether the game pauses after losing window focus. When omitted, only the current state is returned."
                                )
                        )
                ),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "runtimeId", Map.of("type", "string"),
                                "runtimeSide", Map.of("type", "string"),
                                "enabled", Map.of("type", "boolean"),
                                "changed", Map.of("type", "boolean")
                        ),
                        "required", List.of("runtimeId", "runtimeSide", "enabled", "changed")
                ),
                List.of("client", "option"),
                "client",
                true,
                false,
                "public",
                "public"
        );
    }

    private Map<String, Object> payload(ToolCallContext context, Map<String, Object> arguments) {
        var enabledArg = arguments.get("enabled");
        boolean changed = false;
        boolean enabled;
        if (enabledArg instanceof Boolean booleanValue) {
            changed = service.setEnabled(booleanValue);
            enabled = booleanValue;
        } else {
            enabled = service.currentState();
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("runtimeId", context.metadata().get("runtimeId") instanceof String value ? value : "");
        payload.put("runtimeSide", context.side() == null ? "" : context.side());
        payload.put("enabled", enabled);
        payload.put("changed", changed);
        return Map.copyOf(payload);
    }
}
