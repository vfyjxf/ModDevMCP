package dev.vfyjxf.mcp.server.bootstrap;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class GatewayBootstrap {

    @FunctionalInterface
    public interface Probe {
        boolean isReady(BackendMcpEndpointConfig endpoint);
    }

    @FunctionalInterface
    public interface LaunchAction {
        Process launch(GatewayBootstrapConfig config) throws Exception;
    }

    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final Probe probe;
    private final LaunchAction launchAction;
    private final Sleeper sleeper;

    public GatewayBootstrap() {
        this(
                endpoint -> new BackendAvailabilityProbe(Duration.ofMillis(500)).isReady(endpoint.host(), endpoint.port()),
                config -> new BackendProcessLauncher().launch(config),
                duration -> Thread.sleep(duration.toMillis())
        );
    }

    public GatewayBootstrap(Probe probe, LaunchAction launchAction, Sleeper sleeper) {
        this.probe = Objects.requireNonNull(probe, "probe");
        this.launchAction = Objects.requireNonNull(launchAction, "launchAction");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
    }

    public void ensureBackendAvailable(GatewayBootstrapConfig config) {
        var endpoint = new BackendMcpEndpointConfig(config.host(), config.mcpPort());
        if (probe.isReady(endpoint)) {
            return;
        }
        final Process process;
        try {
            process = launchAction.launch(config);
        } catch (Exception exception) {
            throw new IllegalStateException("backend_start_failed: " + exception.getMessage(), exception);
        }
        var deadline = Instant.now().plus(config.startupTimeout());
        while (Instant.now().isBefore(deadline)) {
            if (probe.isReady(endpoint)) {
                return;
            }
            if (process != null && !process.isAlive()) {
                throw new IllegalStateException("backend_start_failed: exit=" + process.exitValue());
            }
            try {
                sleeper.sleep(config.pollInterval());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("backend_start_interrupted", exception);
            }
        }
        throw new IllegalStateException("backend_start_timeout");
    }
}
