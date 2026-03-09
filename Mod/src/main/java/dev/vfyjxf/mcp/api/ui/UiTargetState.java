package dev.vfyjxf.mcp.api.ui;

public record UiTargetState(
        boolean visible,
        boolean enabled,
        boolean focused,
        boolean hovered,
        boolean selected,
        boolean active
) {
    public static UiTargetState defaultState() {
        return new UiTargetState(true, true, false, false, false, false);
    }
}
