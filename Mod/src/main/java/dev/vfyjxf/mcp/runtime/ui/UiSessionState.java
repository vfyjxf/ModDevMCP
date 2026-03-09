package dev.vfyjxf.mcp.runtime.ui;

public record UiSessionState(
        boolean open,
        String focusedTargetId,
        String selectedTargetId,
        String hoveredTargetId,
        String activeTargetId,
        String selectionSource
) {

    public static UiSessionState openedState() {
        return new UiSessionState(true, null, null, null, null, "unknown");
    }

    public static UiSessionState closedState() {
        return new UiSessionState(false, null, null, null, null, "closed");
    }

    public UiSessionState withFocus(String targetId, String source) {
        return new UiSessionState(true, targetId, targetId, hoveredTargetId, targetId, source);
    }

    public UiSessionState reopened(String source) {
        return new UiSessionState(true, null, null, null, null, source);
    }
}
