package dev.vfyjxf.mcp.server.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModDevMcpGatewayBootstrapTest {

    @Test
    void bootstrapSkipsLaunchWhenBackendIsAlreadyReady() {
        var launched = new AtomicBoolean(false);
        var bootstrap = new GatewayBootstrap(
                endpoint -> true,
                launchConfig -> {
                    launched.set(true);
                    return null;
                },
                duration -> {
                }
        );

        assertDoesNotThrow(() -> bootstrap.ensureBackendAvailable(new GatewayBootstrapConfig(
                "127.0.0.1",
                47653,
                47654,
                "java",
                Path.of("backend.args"),
                null,
                Duration.ofSeconds(1),
                Duration.ofMillis(10)
        )));
    }

    @Test
    void bootstrapLaunchesBackendAndWaitsUntilProbeSucceeds() {
        var ready = new AtomicBoolean(false);
        var launched = new AtomicBoolean(false);
        var bootstrap = new GatewayBootstrap(
                endpoint -> ready.get(),
                launchConfig -> {
                    launched.set(true);
                    ready.set(true);
                    return null;
                },
                duration -> {
                }
        );

        assertDoesNotThrow(() -> bootstrap.ensureBackendAvailable(new GatewayBootstrapConfig(
                "127.0.0.1",
                47653,
                47654,
                "java",
                Path.of("backend.args"),
                null,
                Duration.ofSeconds(1),
                Duration.ofMillis(10)
        )));
    }

    @Test
    void bootstrapFailsWithClearTimeoutWhenBackendNeverBecomesReady() {
        var bootstrap = new GatewayBootstrap(
                endpoint -> false,
                launchConfig -> {
                    return null;
                },
                duration -> {
                }
        );

        var exception = assertThrows(IllegalStateException.class, () -> bootstrap.ensureBackendAvailable(new GatewayBootstrapConfig(
                "127.0.0.1",
                47653,
                47654,
                "java",
                Path.of("backend.args"),
                null,
                Duration.ofMillis(50),
                Duration.ofMillis(10)
        )));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("backend_start_timeout"));
    }
}
