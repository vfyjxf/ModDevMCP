package dev.vfyjxf.mcp.runtime.input;

record InputCommand(
        String action,
        double x,
        double y,
        int button,
        int keyCode,
        int scanCode,
        int modifiers,
        String text,
        int durationMs
) {
}
