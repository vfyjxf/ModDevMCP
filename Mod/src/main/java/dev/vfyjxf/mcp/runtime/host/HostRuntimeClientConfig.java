package dev.vfyjxf.mcp.runtime.host;

import dev.vfyjxf.mcp.server.bootstrap.HostEndpointConfig;

public record HostRuntimeClientConfig(
        String host,
        int port,
        long reconnectDelayMs
) {

    private static final long DEFAULT_RECONNECT_DELAY_MS = 1000L;

    public HostRuntimeClientConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (reconnectDelayMs < 0L) {
            throw new IllegalArgumentException("reconnectDelayMs must be non-negative");
        }
    }

    public static HostRuntimeClientConfig loadResolved() {
        var host = HostEndpointConfig.loadResolved();
        return new HostRuntimeClientConfig(host.host(), host.port(), DEFAULT_RECONNECT_DELAY_MS);
    }
}



