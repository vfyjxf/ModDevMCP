package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.ui.SnapshotOptions;
import org.junit.jupiter.api.Test;

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

    private record TestUiContext(String screenClass) implements UiContext {
    }
}
