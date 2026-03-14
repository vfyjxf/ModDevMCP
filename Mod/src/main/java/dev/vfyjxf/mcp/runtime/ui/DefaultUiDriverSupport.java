package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.api.ui.SnapshotOptions;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefaultUiDriverSupport {

    private DefaultUiDriverSupport() {
    }

    public static UiResolveResult resolve(UiDriver driver, UiContext context, UiResolveRequest request) {
        var matches = resolveMatches(driver, context, request);
        if (matches.isEmpty()) {
            return new UiResolveResult("not_found", List.of(), null, "target_not_found", Map.of());
        }
        if (!request.allowMultiple() && matches.size() > 1) {
            return new UiResolveResult("ambiguous", matches, null, "target_ambiguous", Map.of(
                    "matchCount", matches.size()
            ));
        }
        return new UiResolveResult("resolved", matches, matches.getFirst(), null, Map.of());
    }

    public static UiActionabilityResult checkActionability(UiTarget target, String action) {
        if (target == null || !target.state().visible()) {
            return new UiActionabilityResult(false, false, true, false, "target_not_visible", Map.of());
        }
        if (!target.state().enabled()) {
            return new UiActionabilityResult(false, true, false, false, "target_disabled", Map.of());
        }
        if (action == null || action.isBlank() || !target.actions().contains(action)) {
            return new UiActionabilityResult(false, true, true, false, "target_not_actionable", Map.of());
        }
        return new UiActionabilityResult(true, true, true, true, null, Map.of());
    }

    public static UiInspectResult inspect(UiDriver driver, UiContext context, SnapshotOptions options) {
        var snapshot = driver.snapshot(context, options);
        var interactionState = driver.interactionState(context);
        var actionableCount = (int) snapshot.targets().stream()
                .filter(target -> target.state().visible())
                .filter(target -> target.state().enabled())
                .filter(target -> !target.actions().isEmpty())
                .count();
        var interaction = new LinkedHashMap<String, Object>();
        putIfPresent(interaction, "focusedTargetId", interactionState.focusedTarget() == null ? snapshot.focusedTargetId() : interactionState.focusedTarget().targetId());
        putIfPresent(interaction, "hoveredTargetId", interactionState.hoveredTarget() == null ? snapshot.hoveredTargetId() : interactionState.hoveredTarget().targetId());
        putIfPresent(interaction, "activeTargetId", interactionState.activeTarget() == null ? snapshot.activeTargetId() : interactionState.activeTarget().targetId());
        putIfPresent(interaction, "selectedTargetId", interactionState.selectedTarget() == null ? snapshot.selectedTargetId() : interactionState.selectedTarget().targetId());
        interaction.put("cursorX", interactionState.cursorX());
        interaction.put("cursorY", interactionState.cursorY());
        interaction.put("selectionSource", interactionState.selectionSource());
        return new UiInspectResult(
                snapshot.screenClass(),
                snapshot.screenId(),
                snapshot.driverId(),
                Map.of(
                        "targetCount", snapshot.targets().size(),
                        "actionableCount", actionableCount
                ),
                snapshot.targets(),
                interaction,
                null
        );
    }

    public static UiWaitResult waitFor(UiDriver driver, UiContext context, UiWaitRequest request) {
        var startedAt = System.nanoTime();
        while (elapsedMillis(startedAt) <= request.timeoutMs()) {
            var resolved = resolve(driver, context, new UiResolveRequest(request.reference(), false, false, false));
            if (matchesCondition(driver, context, request.condition(), resolved)) {
                return new UiWaitResult(true, elapsedMillis(startedAt), resolved.primary(), null, Map.of(
                        "condition", request.condition()
                ));
            }
            sleepQuietly(request.pollIntervalMs());
        }
        return new UiWaitResult(false, elapsedMillis(startedAt), null, "timeout", Map.of(
                "condition", request.condition()
        ));
    }

    private static boolean matchesCondition(UiDriver driver, UiContext context, String condition, UiResolveResult resolved) {
        if ("targetAppeared".equals(condition)) {
            return "resolved".equals(resolved.status());
        }
        if ("targetGone".equals(condition)) {
            return "not_found".equals(resolved.status());
        }
        if ("screenChanged".equals(condition)) {
            return !driver.matches(context);
        }
        return false;
    }

    private static List<UiTarget> resolveMatches(UiDriver driver, UiContext context, UiResolveRequest request) {
        var reference = request.reference();
        if (reference == null) {
            return List.of();
        }
        if (reference.pointX() != null && reference.pointY() != null) {
            var inspected = driver.inspectAt(context, reference.pointX(), reference.pointY());
            if (inspected.accepted() && inspected.value() != null) {
                return filterState(inspected.value(), request);
            }
        }
        var snapshot = driver.snapshot(context, SnapshotOptions.DEFAULT);
        return filterState(snapshot.targets().stream()
                .filter(target -> matchesReference(target, reference))
                .toList(), request);
    }

    private static List<UiTarget> filterState(List<UiTarget> targets, UiResolveRequest request) {
        return targets.stream()
                .filter(target -> request.includeHidden() || target.state().visible())
                .filter(target -> request.includeDisabled() || target.state().enabled())
                .toList();
    }

    private static boolean matchesReference(UiTarget target, UiTargetReference reference) {
        if (reference.ref() != null && !reference.ref().isBlank()) {
            return reference.ref().equals(target.targetId());
        }
        var locator = reference.locator();
        if (locator == null) {
            return false;
        }
        return matchesLocator(target, locator);
    }

    private static boolean matchesLocator(UiTarget target, UiLocator locator) {
        if (locator.role() != null && !locator.role().equals(target.role())) {
            return false;
        }
        if (locator.text() != null && !locator.text().equals(target.text())) {
            return false;
        }
        if (locator.containsText() != null && (target.text() == null || !target.text().contains(locator.containsText()))) {
            return false;
        }
        if (locator.id() != null && !locator.id().equals(target.targetId())) {
            return false;
        }
        return true;
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private static void sleepQuietly(long pollIntervalMs) {
        if (pollIntervalMs <= 0L) {
            return;
        }
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for ui condition", exception);
        }
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
