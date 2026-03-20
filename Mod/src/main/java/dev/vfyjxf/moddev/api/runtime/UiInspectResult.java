package dev.vfyjxf.moddev.api.runtime;

import dev.vfyjxf.moddev.api.ui.TooltipSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;

import java.util.List;
import java.util.Map;

/**
 * Concise driver inspection payload returned by high-level inspect flows.
 */
public record UiInspectResult(
        String screen,
        String screenId,
        String driverId,
        Map<String, Object> summary,
        List<UiTarget> targets,
        Map<String, Object> interaction,
        TooltipSnapshot tooltip
) {
    public UiInspectResult {
        summary = summary == null ? Map.of() : Map.copyOf(summary);
        targets = targets == null ? List.of() : List.copyOf(targets);
        interaction = interaction == null ? Map.of() : Map.copyOf(interaction);
    }
}

