package dev.vfyjxf.mcp.api.runtime;

/**
 * Input for a driver-level wait operation.
 */
public record UiWaitRequest(
        UiTargetReference reference,
        String condition,
        long timeoutMs,
        long pollIntervalMs,
        long stableForMs
) {
}
