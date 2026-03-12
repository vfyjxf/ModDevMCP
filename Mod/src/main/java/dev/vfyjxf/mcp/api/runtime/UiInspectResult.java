package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.ui.TooltipSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.List;
import java.util.Map;

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
