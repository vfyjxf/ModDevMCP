package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.ui.SnapshotOptions;
import dev.vfyjxf.moddev.api.ui.TargetSelector;
import dev.vfyjxf.moddev.api.ui.UiActionRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackRegionUiDriverTest {

    @Test
    void fallbackDriverMatchesUnknownScreensAndReturnsViewportTarget() {
        var driver = new FallbackRegionUiDriver();
        var context = new TestUiContext("custom.UnknownScreen");

        assertTrue(driver.matches(context));

        var snapshot = driver.snapshot(context, SnapshotOptions.DEFAULT);

        assertEquals("fallback-region", snapshot.driverId());
        assertFalse(snapshot.targets().isEmpty());
    }

    @Test
    void clickActionSucceedsForViewportTarget() {
        var driver = new FallbackRegionUiDriver();
        var context = new TestUiContext("custom.UnknownScreen");

        var result = driver.action(
                context,
                new UiActionRequest(TargetSelector.builder().id("viewport").build(), "click", Map.of())
        );

        assertTrue(result.accepted());
        assertTrue(result.performed());
        assertEquals("click", result.value().get("action"));
        assertEquals(Map.of("id", "viewport"), result.value().get("target"));
    }

    private record TestUiContext(String screenClass) implements UiContext {
    }
}
