package dev.vfyjxf.moddev.api.ui;

import java.util.List;
import java.util.Map;

/**
 * Snapshot of the current screen as understood by one UI driver or an aggregated driver composition.
 */
public record UiSnapshot(
        String screenId,
        String screenClass,
        String driverId,
        List<UiTarget> targets,
        List<Map<String, Object>> overlays,
        String focusedTargetId,
        String selectedTargetId,
        String hoveredTargetId,
        String activeTargetId,
        Map<String, Object> extensions
) {
    public UiSnapshot {
        targets = targets == null ? List.of() : List.copyOf(targets);
        overlays = overlays == null ? List.of() : List.copyOf(overlays);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}

