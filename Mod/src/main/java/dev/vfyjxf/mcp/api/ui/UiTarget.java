package dev.vfyjxf.mcp.api.ui;

import java.util.List;
import java.util.Map;

/**
 * Canonical description of a single UI element exposed by a driver.
 */
public record UiTarget(
        String targetId,
        String driverId,
        String screenClass,
        String modId,
        String role,
        String text,
        Bounds bounds,
        UiTargetState state,
        List<String> actions,
        Map<String, Object> extensions
) {
    public UiTarget {
        actions = actions == null ? List.of() : List.copyOf(actions);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}
