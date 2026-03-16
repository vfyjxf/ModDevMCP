package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.Map;

/**
 * Result returned by a driver wait operation.
 */
public record UiWaitResult(
        boolean matched,
        long elapsedMs,
        UiTarget matchedTarget,
        String errorCode,
        Map<String, Object> details
) {
    public UiWaitResult {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
