package dev.vfyjxf.moddev.api.ui;

import java.util.List;
import java.util.Map;

public record TooltipSnapshot(
        String targetId,
        List<String> lines,
        Bounds bounds,
        Map<String, Object> extensions
) {
    public TooltipSnapshot {
        lines = lines == null ? List.of() : List.copyOf(lines);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}

