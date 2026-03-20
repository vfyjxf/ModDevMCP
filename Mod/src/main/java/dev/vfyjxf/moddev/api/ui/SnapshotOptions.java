package dev.vfyjxf.moddev.api.ui;

public record SnapshotOptions(
        boolean includeElements,
        boolean includeText,
        boolean includeSlots,
        boolean includeOverlays
) {
    public static final SnapshotOptions DEFAULT = new SnapshotOptions(true, true, true, true);
}

