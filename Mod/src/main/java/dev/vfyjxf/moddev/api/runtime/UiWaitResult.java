package dev.vfyjxf.moddev.api.runtime;

import dev.vfyjxf.moddev.api.ui.UiTarget;

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

