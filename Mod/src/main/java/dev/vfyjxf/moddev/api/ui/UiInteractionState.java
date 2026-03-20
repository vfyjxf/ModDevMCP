package dev.vfyjxf.moddev.api.ui;

public record UiInteractionState(
        UiTarget focusedTarget,
        UiTarget selectedTarget,
        UiTarget hoveredTarget,
        UiTarget activeTarget,
        int cursorX,
        int cursorY,
        boolean textInputActive,
        String selectionSource,
        String driverId
) {
}

