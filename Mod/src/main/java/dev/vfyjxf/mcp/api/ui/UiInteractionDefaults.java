package dev.vfyjxf.mcp.api.ui;

public record UiInteractionDefaults(
        String focusedTargetId,
        String selectedTargetId,
        String hoveredTargetId,
        String activeTargetId,
        String selectionSource
) {

    public static UiInteractionDefaults empty() {
        return new UiInteractionDefaults(null, null, null, null, "unknown");
    }
}
