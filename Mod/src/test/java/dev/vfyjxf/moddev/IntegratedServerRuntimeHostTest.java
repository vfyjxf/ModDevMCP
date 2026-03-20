package dev.vfyjxf.moddev;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegratedServerRuntimeHostTest {

    @Test
    void startsAndStopsOnlyForIntegratedServerLifecycle() {
        var startCount = new AtomicInteger();
        var stopCount = new AtomicInteger();
        var host = new IntegratedServerRuntimeHost(startCount::incrementAndGet, stopCount::incrementAndGet);

        host.handleServerStarted(true);
        host.handleServerStarted(false);
        host.handleServerStarted(false);
        host.handleServerStopping(true);
        host.handleServerStopping(false);
        host.handleServerStopping(false);

        assertEquals(1, startCount.get());
        assertEquals(1, stopCount.get());
    }

    @Test
    void closeStopsActiveIntegratedServerRuntimeOnce() {
        var startCount = new AtomicInteger();
        var stopCount = new AtomicInteger();
        var host = new IntegratedServerRuntimeHost(startCount::incrementAndGet, stopCount::incrementAndGet);

        host.handleServerStarted(false);
        host.close();
        host.close();

        assertEquals(1, startCount.get());
        assertEquals(1, stopCount.get());
    }
}

