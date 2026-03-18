package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.api.ui.Bounds;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;
import dev.vfyjxf.mcp.api.ui.UiTargetState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UiAutomationSessionManagerTest {

    @Test
    void openCreatesSessionWithRefsAndResolvesTargetOnSameScreen() {
        var manager = new UiAutomationSessionManager();

        var session = manager.open(snapshot(
                "screen-1",
                "custom.ScreenOne",
                target("target-a", "Button A"),
                target("target-b", "Button B")
        ));

        assertNotNull(session);
        assertFalse(session.sessionId().isBlank());
        assertEquals(2, session.refs().size());

        var firstRef = session.refs().getFirst();
        var resolved = manager.resolveTarget(session.sessionId(), firstRef.refId());

        assertTrue(resolved.accepted());
        assertEquals("target-a", resolved.value().targetId());
    }

    @Test
    void refreshReportsScreenChangedAndMarksMissingTargetRefAsStale() {
        var manager = new UiAutomationSessionManager();

        var session = manager.open(snapshot(
                "screen-1",
                "custom.ScreenOne",
                target("target-a", "Button A")
        ));
        var firstRef = session.refs().getFirst();

        var refresh = manager.refresh(session.sessionId(), snapshot(
                "screen-2",
                "custom.ScreenTwo",
                target("target-b", "Button B")
        )).orElseThrow();

        assertTrue(refresh.screenChanged());
        assertEquals("custom.ScreenTwo", refresh.session().snapshot().screenClass());

        var resolved = manager.resolveTarget(session.sessionId(), firstRef.refId());

        assertFalse(resolved.accepted());
        assertEquals("target_stale", resolved.reason());
    }

    @Test
    void markedStaleSessionRejectsOldRefsUntilRefreshed() {
        var manager = new UiAutomationSessionManager();

        var session = manager.open(snapshot(
                "screen-1",
                "custom.ScreenOne",
                target("target-a", "Button A")
        ));
        var firstRef = session.refs().getFirst();

        assertTrue(manager.markStale(session.sessionId()));

        var staleResult = manager.resolveTarget(session.sessionId(), firstRef.refId());

        assertFalse(staleResult.accepted());
        assertEquals("session_stale", staleResult.reason());

        var refresh = manager.refresh(session.sessionId(), snapshot(
                "screen-1",
                "custom.ScreenOne",
                target("target-a", "Button A")
        )).orElseThrow();
        var resolved = manager.resolveTarget(refresh.session().sessionId(), firstRef.refId());

        assertTrue(resolved.accepted());
        assertEquals("target-a", resolved.value().targetId());
    }

    @Test
    void openCreatesDistinctRefsForSameTargetIdAcrossDrivers() {
        var manager = new UiAutomationSessionManager();

        var session = manager.open(snapshot(
                "screen-1",
                "custom.ScreenOne",
                target("base", "shared-id", "Base Button"),
                target("addon", "shared-id", "Addon Button")
        ));

        assertEquals(2, session.refs().size());
        assertEquals(
                Set.of("base", "addon"),
                session.refs().stream().map(UiAutomationRef::driverId).collect(java.util.stream.Collectors.toSet())
        );
        assertNotEquals(session.refs().get(0).refId(), session.refs().get(1).refId());
    }

    private UiSnapshot snapshot(String screenId, String screenClass, UiTarget... targets) {
        return new UiSnapshot(
                screenId,
                screenClass,
                "test-driver",
                List.of(targets),
                List.of(),
                null,
                null,
                null,
                null,
                Map.of()
        );
    }

    private UiTarget target(String targetId, String text) {
        return target("test-driver", targetId, text);
    }

    private UiTarget target(String driverId, String targetId, String text) {
        return new UiTarget(
                targetId,
                driverId,
                "custom.Screen",
                "minecraft",
                "button",
                text,
                new Bounds(0, 0, 20, 20),
                UiTargetState.defaultState(),
                List.of("click"),
                Map.of()
        );
    }
}
