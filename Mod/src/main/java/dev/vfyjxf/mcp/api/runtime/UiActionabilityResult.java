package dev.vfyjxf.mcp.api.runtime;

import java.util.Map;

public record UiActionabilityResult(
        boolean actionable,
        boolean visible,
        boolean enabled,
        boolean supported,
        String errorCode,
        Map<String, Object> details
) {
    public UiActionabilityResult {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
