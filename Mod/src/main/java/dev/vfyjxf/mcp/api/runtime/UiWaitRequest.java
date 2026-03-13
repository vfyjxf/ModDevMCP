package dev.vfyjxf.mcp.api.runtime;

public record UiWaitRequest(
        UiTargetReference reference,
        String condition,
        long timeoutMs,
        long pollIntervalMs,
        long stableForMs
) {
}
