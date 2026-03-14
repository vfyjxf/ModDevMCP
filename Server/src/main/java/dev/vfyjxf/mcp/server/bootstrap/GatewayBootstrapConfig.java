package dev.vfyjxf.mcp.server.bootstrap;

import java.nio.file.Path;
import java.time.Duration;

public record GatewayBootstrapConfig(
        String host,
        int runtimePort,
        int mcpPort,
        String javaCommand,
        Path backendArgsFile,
        Path backendLauncher,
        Duration startupTimeout,
        Duration pollInterval
) {

    public static final String BACKEND_JAVA_COMMAND_PROPERTY = "moddevmcp.backend.javaCommand";
    public static final String BACKEND_ARGS_FILE_PROPERTY = "moddevmcp.backend.argsFile";
    public static final String BACKEND_LAUNCHER_PROPERTY = "moddevmcp.backend.launcher";
    public static final String BACKEND_START_TIMEOUT_MS_PROPERTY = "moddevmcp.backend.startTimeoutMs";
    public static final String BACKEND_POLL_INTERVAL_MS_PROPERTY = "moddevmcp.backend.pollIntervalMs";

    public GatewayBootstrapConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (runtimePort < 1 || runtimePort > 65535) {
            throw new IllegalArgumentException("runtimePort must be between 1 and 65535");
        }
        if (mcpPort < 1 || mcpPort > 65535) {
            throw new IllegalArgumentException("mcpPort must be between 1 and 65535");
        }
        if (javaCommand == null || javaCommand.isBlank()) {
            throw new IllegalArgumentException("javaCommand must not be blank");
        }
        if (backendArgsFile == null) {
            throw new IllegalArgumentException("backendArgsFile must not be null");
        }
        if (startupTimeout.isNegative() || startupTimeout.isZero()) {
            throw new IllegalArgumentException("startupTimeout must be positive");
        }
        if (pollInterval.isNegative() || pollInterval.isZero()) {
            throw new IllegalArgumentException("pollInterval must be positive");
        }
    }

    public static GatewayBootstrapConfig loadResolved() {
        var runtime = HostEndpointConfig.loadResolved();
        var mcp = BackendMcpEndpointConfig.loadResolved();
        var javaCommand = System.getProperty(BACKEND_JAVA_COMMAND_PROPERTY, "java");
        var backendArgsFile = Path.of(System.getProperty(BACKEND_ARGS_FILE_PROPERTY, ""));
        var launcherValue = System.getProperty(BACKEND_LAUNCHER_PROPERTY, "");
        var backendLauncher = launcherValue == null || launcherValue.isBlank() ? null : Path.of(launcherValue);
        var startupTimeout = Duration.ofMillis(Long.getLong(BACKEND_START_TIMEOUT_MS_PROPERTY, 60000L));
        var pollInterval = Duration.ofMillis(Long.getLong(BACKEND_POLL_INTERVAL_MS_PROPERTY, 200L));
        return new GatewayBootstrapConfig(
                runtime.host(),
                runtime.port(),
                mcp.port(),
                javaCommand,
                backendArgsFile,
                backendLauncher,
                startupTimeout,
                pollInterval
        );
    }
}
