package dev.vfyjxf.mcp.runtime.tool;

public record UiAutomationTraceEntry(
        int stepIndex,
        String type,
        long elapsedMs,
        boolean success,
        String errorCode,
        String errorMessage
) {
}
