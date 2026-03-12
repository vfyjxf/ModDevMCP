package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.api.ui.UiSnapshot;

import java.util.List;

public record UiAutomationSession(
        String sessionId,
        UiSnapshot snapshot,
        List<UiAutomationRef> refs,
        boolean stale
) {
    public UiAutomationSession {
        refs = refs == null ? List.of() : List.copyOf(refs);
    }
}
