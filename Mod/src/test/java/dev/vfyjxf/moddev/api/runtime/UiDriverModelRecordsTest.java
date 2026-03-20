package dev.vfyjxf.moddev.api.runtime;

import dev.vfyjxf.moddev.api.ui.Bounds;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import dev.vfyjxf.moddev.api.ui.UiTargetState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UiDriverModelRecordsTest {

    @Test
    void locatorRetainsPlaywrightStyleFields() {
        var locator = new UiLocator(
                "button",
                "Create New World",
                "World",
                "button-create-new-world",
                0,
                "screen-root"
        );

        assertEquals("button", locator.role());
        assertEquals("Create New World", locator.text());
        assertEquals("World", locator.containsText());
        assertEquals("button-create-new-world", locator.id());
        assertEquals(0, locator.index());
        assertEquals("screen-root", locator.scopeRef());
    }

    @Test
    void targetReferenceSupportsRefLocatorAndPointFactories() {
        var locator = new UiLocator("button", null, null, null, null, null);

        var byRef = UiTargetReference.ref("ref-1");
        var byLocator = UiTargetReference.locator(locator);
        var byPoint = UiTargetReference.point(120, 64);

        assertEquals("ref-1", byRef.ref());
        assertNull(byRef.locator());
        assertNull(byRef.pointX());
        assertNull(byRef.pointY());

        assertNull(byLocator.ref());
        assertEquals(locator, byLocator.locator());
        assertNull(byLocator.pointX());
        assertNull(byLocator.pointY());

        assertNull(byPoint.ref());
        assertNull(byPoint.locator());
        assertEquals(120, byPoint.pointX());
        assertEquals(64, byPoint.pointY());
    }

    @Test
    void resolveAndActionabilityResultsNormalizeCollectionsAndDetails() {
        var target = target("button-create-world", "Create");
        var resolveResult = new UiResolveResult(
                "resolved",
                null,
                target,
                null,
                null
        );
        var actionabilityResult = new UiActionabilityResult(
                true,
                true,
                true,
                true,
                null,
                null
        );

        assertNotNull(resolveResult.matches());
        assertTrue(resolveResult.matches().isEmpty());
        assertEquals(target, resolveResult.primary());
        assertTrue(actionabilityResult.actionable());
        assertTrue(actionabilityResult.visible());
        assertTrue(actionabilityResult.enabled());
        assertTrue(actionabilityResult.supported());
        assertEquals(Map.of(), actionabilityResult.details());
    }

    @Test
    void inspectAndWaitResultsKeepConciseStablePayloads() {
        var target = target("button-create-world", "Create");
        var inspectResult = new UiInspectResult(
                "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen",
                "screen-1",
                "vanilla-screen",
                null,
                null,
                null,
                null
        );
        var waitRequest = new UiWaitRequest(
                UiTargetReference.ref("ref-1"),
                "targetAppeared",
                2_000L,
                50L,
                100L
        );
        var waitResult = new UiWaitResult(
                true,
                120L,
                target,
                null,
                null
        );

        assertEquals(Map.of(), inspectResult.summary());
        assertEquals(List.of(), inspectResult.targets());
        assertEquals(Map.of(), inspectResult.interaction());
        assertEquals("targetAppeared", waitRequest.condition());
        assertEquals(2_000L, waitRequest.timeoutMs());
        assertEquals(50L, waitRequest.pollIntervalMs());
        assertEquals(100L, waitRequest.stableForMs());
        assertTrue(waitResult.matched());
        assertEquals(120L, waitResult.elapsedMs());
        assertEquals(target, waitResult.matchedTarget());
        assertFalse(waitResult.details().containsKey("rawSnapshot"));
    }

    private UiTarget target(String targetId, String text) {
        return new UiTarget(
                targetId,
                "vanilla-screen",
                "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen",
                "minecraft",
                "button",
                text,
                new Bounds(0, 0, 120, 20),
                UiTargetState.defaultState(),
                List.of("click"),
                Map.of()
        );
    }
}

