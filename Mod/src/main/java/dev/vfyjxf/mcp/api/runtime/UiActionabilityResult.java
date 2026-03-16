package dev.vfyjxf.mcp.api.runtime;

import java.util.Map;

/**
 * Result of checking whether a target can accept a specific action.
 */
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
