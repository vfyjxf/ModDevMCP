package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.List;
import java.util.Map;

/**
 * Result of resolving a UI reference or locator into concrete targets.
 */
public record UiResolveResult(
        String status,
        List<UiTarget> matches,
        UiTarget primary,
        String errorCode,
        Map<String, Object> details
) {
    public UiResolveResult {
        matches = matches == null ? List.of() : List.copyOf(matches);
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
