package dev.vfyjxf.moddev.runtime.world;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LiveClientWorldServiceTest {

    @Test
    void awaitWorldLoadRetriesTransientUnavailableUntilLoaded() {
        var attempts = new AtomicInteger();
        var time = new AtomicLong();

        LiveClientWorldService.awaitWorldLoad(
                () -> {
                    if (attempts.getAndIncrement() < 2) {
                        throw new WorldServiceException("world_action_unavailable", "render thread busy");
                    }
                    return true;
                },
                "world_create_failed",
                new AtomicReference<>(),
                time::get,
                millis -> time.addAndGet(TimeUnit.MILLISECONDS.toNanos(millis)),
                TimeUnit.SECONDS.toNanos(30)
        );

        assertEquals(3, attempts.get());
    }

    @Test
    void awaitWorldLoadFailsFastForNonTransientWorldErrors() {
        var exception = assertThrows(
                WorldServiceException.class,
                () -> LiveClientWorldService.awaitWorldLoad(
                        () -> {
                            throw new WorldServiceException("world_create_failed", "broken");
                        },
                        "world_create_failed",
                        new AtomicReference<>(),
                        System::nanoTime,
                        millis -> { },
                        TimeUnit.SECONDS.toNanos(30)
                )
        );

        assertEquals("world_create_failed", exception.errorCode());
    }

    @Test
    void dispatchAsyncDefersCreateFlowAndCapturesFailureMessage() {
        var scheduled = new AtomicReference<Runnable>();
        var failure = new AtomicReference<String>();

        LiveClientWorldService.dispatchAsync(
                scheduled::set,
                failure,
                () -> {
                    throw new WorldServiceException("world_create_failed", "broken");
                }
        );

        assertNull(failure.get());
        scheduled.get().run();
        assertEquals("broken", failure.get());
    }
}
