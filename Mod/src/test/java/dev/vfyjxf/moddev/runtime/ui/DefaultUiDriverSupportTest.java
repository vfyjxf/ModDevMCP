package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.*;
import dev.vfyjxf.moddev.api.ui.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DefaultUiDriverSupportTest {

    @Test
    void resolveFindsTargetByLocatorText() {
        var driver = new MutableDriver(List.of(
                target("button-create-world", "Create New World", true, true, List.of("click")),
                target("button-cancel", "Cancel", true, true, List.of("click"))
        ));

        var result = driver.resolve(new TestUiContext(), new UiResolveRequest(
                UiTargetReference.locator(new UiLocator("button", "Create New World", null, null, null, null)),
                false,
                false,
                false
        ));

        assertEquals("resolved", result.status());
        assertEquals("button-create-world", result.primary().targetId());
        assertEquals(1, result.matches().size());
    }

    @Test
    void resolveReportsAmbiguousMatches() {
        var driver = new MutableDriver(List.of(
                target("button-create-world", "Create New World", true, true, List.of("click")),
                target("button-create-experiment", "Create Experimental World", true, true, List.of("click"))
        ));

        var result = driver.resolve(new TestUiContext(), new UiResolveRequest(
                UiTargetReference.locator(new UiLocator("button", null, "Create", null, null, null)),
                false,
                false,
                false
        ));

        assertEquals("ambiguous", result.status());
        assertEquals("target_ambiguous", result.errorCode());
        assertEquals(2, result.matches().size());
    }

    @Test
    void checkActionabilityRejectsHiddenDisabledAndUnsupportedActions() {
        var driver = new MutableDriver(List.of());

        UiActionabilityResult hidden = driver.checkActionability(
                new TestUiContext(),
                target("hidden-button", "Hidden", false, true, List.of("click")),
                "click"
        );
        UiActionabilityResult disabled = driver.checkActionability(
                new TestUiContext(),
                target("disabled-button", "Disabled", true, false, List.of("click")),
                "click"
        );
        UiActionabilityResult unsupported = driver.checkActionability(
                new TestUiContext(),
                target("hover-only", "Hover", true, true, List.of("hover")),
                "click"
        );

        assertFalse(hidden.actionable());
        assertEquals("target_not_visible", hidden.errorCode());
        assertFalse(disabled.actionable());
        assertEquals("target_disabled", disabled.errorCode());
        assertFalse(unsupported.actionable());
        assertEquals("target_not_actionable", unsupported.errorCode());
    }

    @Test
    void inspectBuildsSummaryAndInteractionSnapshot() {
        var primary = target("button-create-world", "Create New World", true, true, List.of("click"));
        var secondary = target("button-cancel", "Cancel", true, false, List.of("click"));
        var driver = new MutableDriver(List.of(primary, secondary));

        UiInspectResult result = driver.inspect(new TestUiContext(), SnapshotOptions.DEFAULT);

        assertEquals("custom.Screen", result.screen());
        assertEquals("screen-1", result.screenId());
        assertEquals("mutable-driver", result.driverId());
        assertEquals(2, result.summary().get("targetCount"));
        assertEquals(1, result.summary().get("actionableCount"));
        assertEquals("button-create-world", result.interaction().get("focusedTargetId"));
        assertEquals("button-cancel", result.interaction().get("hoveredTargetId"));
    }

    @Test
    void waitForPollsUntilTargetAppears() {
        var driver = new MutableDriver(new CopyOnWriteArrayList<>());
        Thread.ofVirtual().start(() -> {
            sleepQuietly(60);
            driver.targets.add(target("button-create-world", "Create New World", true, true, List.of("click")));
        });

        var result = driver.waitFor(new TestUiContext(), new UiWaitRequest(
                UiTargetReference.locator(new UiLocator("button", "Create New World", null, null, null, null)),
                "targetAppeared",
                500L,
                20L,
                0L
        ));

        assertTrue(result.matched());
        assertNotNull(result.matchedTarget());
        assertEquals("button-create-world", result.matchedTarget().targetId());
    }

    @Test
    void waitForReturnsTimeoutWhenConditionDoesNotMatch() {
        var driver = new MutableDriver(List.of());

        var result = driver.waitFor(new TestUiContext(), new UiWaitRequest(
                UiTargetReference.locator(new UiLocator("button", "Create New World", null, null, null, null)),
                "targetAppeared",
                120L,
                20L,
                0L
        ));

        assertFalse(result.matched());
        assertEquals("timeout", result.errorCode());
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static UiTarget target(String targetId, String text, boolean visible, boolean enabled, List<String> actions) {
        return new UiTarget(
                targetId,
                "mutable-driver",
                "custom.Screen",
                "minecraft",
                "button",
                text,
                new Bounds(10, 20, 100, 20),
                new UiTargetState(visible, enabled, false, false, false, false),
                actions,
                Map.of()
        );
    }

    private record TestUiContext() implements UiContext {
        @Override
        public String screenClass() {
            return "custom.Screen";
        }
    }

    private static final class MutableDriver implements UiDriver {

        private static final DriverDescriptor DESCRIPTOR = new DriverDescriptor(
                "mutable-driver",
                "test",
                1_000,
                Set.of("snapshot", "query")
        );

        private final List<UiTarget> targets;

        private MutableDriver(List<UiTarget> targets) {
            this.targets = new ArrayList<>(targets);
        }

        @Override
        public DriverDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public boolean matches(UiContext context) {
            return "custom.Screen".equals(context.screenClass());
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot(
                    "screen-1",
                    context.screenClass(),
                    DESCRIPTOR.id(),
                    List.copyOf(targets),
                    List.of(),
                    "button-create-world",
                    null,
                    "button-cancel",
                    null,
                    Map.of()
            );
        }

        @Override
        public List<UiTarget> query(UiContext context, TargetSelector selector) {
            return targets.stream()
                    .filter(target -> selector.role() == null || selector.role().equals(target.role()))
                    .filter(target -> selector.id() == null || selector.id().equals(target.targetId()))
                    .filter(target -> selector.text() == null || selector.text().equals(target.text()))
                    .toList();
        }

        @Override
        public UiInteractionState interactionState(UiContext context) {
            var snapshot = snapshot(context, SnapshotOptions.DEFAULT);
            return new UiInteractionState(
                    snapshot.targets().stream().filter(target -> target.targetId().equals(snapshot.focusedTargetId())).findFirst().orElse(null),
                    null,
                    snapshot.targets().stream().filter(target -> target.targetId().equals(snapshot.hoveredTargetId())).findFirst().orElse(null),
                    null,
                    0,
                    0,
                    false,
                    "test",
                    DESCRIPTOR.id()
            );
        }
    }
}

