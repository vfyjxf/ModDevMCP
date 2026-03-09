package dev.vfyjxf.mcp.api.ui;

import java.util.Map;

public record UiActionRequest(
        TargetSelector target,
        String action,
        Map<String, Object> arguments
) {
}
