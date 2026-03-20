package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.model.OperationResult;
import dev.vfyjxf.moddev.api.runtime.DriverDescriptor;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiDriver;
import dev.vfyjxf.moddev.api.ui.*;
import dev.vfyjxf.moddev.runtime.UiInteractionStateResolverRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FallbackRegionUiDriver implements UiDriver {

    private static final DriverDescriptor DESCRIPTOR = new DriverDescriptor(
            "fallback-region",
            "moddev",
            0,
            Set.of("snapshot", "query", "capture", "inspect")
    );
    private final UiSessionStateRegistry sessionStates;
    private final UiInteractionStateResolverRegistry interactionResolvers;

    public FallbackRegionUiDriver() {
        this(new UiSessionStateRegistry(), BuiltinUiInteractionResolvers.newRegistry());
    }

    public FallbackRegionUiDriver(UiSessionStateRegistry sessionStates, UiInteractionStateResolverRegistry interactionResolvers) {
        this.sessionStates = sessionStates;
        this.interactionResolvers = interactionResolvers;
    }

    @Override
    public DriverDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean matches(UiContext context) {
        return true;
    }

    @Override
    public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
        var sessionState = sessionStates.stateFor(context, DESCRIPTOR.id());
        if (!sessionState.open()) {
            return new UiSnapshot("screen", context.screenClass(), DESCRIPTOR.id(), List.of(), List.of(), null, null, null, null, Map.of("fallback", true, "closed", true));
        }
        var defaults = interactionResolvers.resolve(DESCRIPTOR.id(), context, List.of());
        var viewport = new UiTarget(
                "viewport",
                DESCRIPTOR.id(),
                context.screenClass(),
                context.modId(),
                "region",
                "Viewport",
                new Bounds(0, 0, 320, 240),
                UiTargetState.defaultState(),
                List.of("capture", "click"),
                Map.of("fallback", true)
        );
        return new UiSnapshot("screen", context.screenClass(), DESCRIPTOR.id(), List.of(viewport), List.of(),
                effectiveTargetId(sessionState.focusedTargetId(), defaults.focusedTargetId()),
                effectiveTargetId(sessionState.selectedTargetId(), defaults.selectedTargetId()),
                effectiveTargetId(sessionState.hoveredTargetId(), defaults.hoveredTargetId()),
                effectiveTargetId(sessionState.activeTargetId(), defaults.activeTargetId()),
                Map.of("fallback", true));
    }

    @Override
    public List<UiTarget> query(UiContext context, TargetSelector selector) {
        return snapshot(context, SnapshotOptions.DEFAULT).targets().stream()
                .filter(target -> matchesTarget(selector, target))
                .toList();
    }

    private boolean matchesTarget(TargetSelector selector, UiTarget target) {
        return matchesScope(selector, target)
                && (selector.role() == null || selector.role().equals(target.role()))
                && (selector.id() == null || selector.id().equals(target.targetId()))
                && (selector.text() == null || selector.text().equals(target.text()))
                && (selector.modId() == null || selector.modId().equals(target.modId()))
                && intersects(selector, target);
    }

    private boolean matchesScope(TargetSelector selector, UiTarget target) {
        if (selector.scope() == null || selector.scope().isBlank() || "element".equals(selector.scope())) {
            return true;
        }
        return selector.scope().equals(target.role());
    }

    private boolean intersects(TargetSelector selector, UiTarget target) {
        if (selector.bounds() == null) {
            return true;
        }
        var a = selector.bounds();
        var b = target.bounds();
        return a.x() < b.x() + b.width()
                && a.x() + a.width() > b.x()
                && a.y() < b.y() + b.height()
                && a.y() + a.height() > b.y();
    }

    @Override
    public OperationResult<Map<String, Object>> capture(UiContext context, CaptureRequest request) {
        var captured = resolveTargets(context, request.target());
        var excluded = resolveTargets(context, request.exclude());
        var filtered = captured.stream()
                .filter(target -> excluded.stream().noneMatch(excludedTarget -> excludedTarget.targetId().equals(target.targetId())))
                .toList();
        return OperationResult.success(Map.of(
                "driverId", DESCRIPTOR.id(),
                "mode", request.mode(),
                "capturedTargets", filtered.stream().map(this::targetToMap).toList(),
                "excludedTargets", excluded.stream().map(this::targetToMap).toList()
        ));
    }

    @Override
    public OperationResult<Map<String, Object>> action(UiContext context, UiActionRequest request) {
        if ("close".equals(request.action())) {
            sessionStates.update(context, DESCRIPTOR.id(), UiSessionState.closedState());
            return OperationResult.success(Map.of(
                    "driverId", DESCRIPTOR.id(),
                    "action", request.action(),
                    "performed", true,
                    "target", selectorToMap(request.target())
            ));
        }
        if ("click".equals(request.action()) || "switch".equals(request.action())) {
            var target = query(context, request.target()).stream().findFirst().orElse(snapshot(context, SnapshotOptions.DEFAULT).targets().getFirst());
            sessionStates.update(context, DESCRIPTOR.id(), UiSessionState.openedState().withActive(target.targetId(), "programmatic"));
            return OperationResult.success(Map.of(
                    "driverId", DESCRIPTOR.id(),
                    "action", request.action(),
                    "performed", true,
                    "target", selectorToMap(request.target()),
                    "resolvedTargetId", target.targetId()
            ));
        }
        if ("run_intent".equals(request.action())) {
            return OperationResult.rejected("unsupported_intent");
        }
        return OperationResult.rejected("unsupported_action");
    }

    @Override
    public OperationResult<List<UiTarget>> inspectAt(UiContext context, int x, int y) {
        var matches = snapshot(context, SnapshotOptions.DEFAULT).targets().stream()
                .filter(target -> contains(target, x, y))
                .toList();
        return OperationResult.success(matches);
    }

    @Override
    public OperationResult<TooltipSnapshot> tooltip(UiContext context, TargetSelector selector) {
        var target = query(context, selector).stream().findFirst().orElse(snapshot(context, SnapshotOptions.DEFAULT).targets().getFirst());
        return OperationResult.success(new TooltipSnapshot(
                target.targetId(),
                List.of(target.text().isBlank() ? "Viewport" : target.text()),
                target.bounds(),
                Map.of("driverId", DESCRIPTOR.id())
        ));
    }

    @Override
    public dev.vfyjxf.moddev.api.ui.UiInteractionState interactionState(UiContext context) {
        var sessionState = sessionStates.stateFor(context, DESCRIPTOR.id());
        if (!sessionState.open()) {
            return new dev.vfyjxf.moddev.api.ui.UiInteractionState(null, null, null, null, context.mouseX(), context.mouseY(), false, "closed", DESCRIPTOR.id());
        }
        var snapshot = snapshot(context, SnapshotOptions.DEFAULT);
        return new dev.vfyjxf.moddev.api.ui.UiInteractionState(
                findTarget(snapshot.targets(), snapshot.focusedTargetId()),
                findTarget(snapshot.targets(), snapshot.selectedTargetId()),
                findTarget(snapshot.targets(), snapshot.hoveredTargetId()),
                findTarget(snapshot.targets(), snapshot.activeTargetId()),
                context.mouseX(),
                context.mouseY(),
                false,
                "unknown".equals(sessionState.selectionSource())
                        ? interactionResolvers.resolve(DESCRIPTOR.id(), context, snapshot.targets()).selectionSource()
                        : sessionState.selectionSource(),
                DESCRIPTOR.id()
        );
    }

    private boolean contains(UiTarget target, int x, int y) {
        var bounds = target.bounds();
        return x >= bounds.x() && x < bounds.x() + bounds.width()
                && y >= bounds.y() && y < bounds.y() + bounds.height();
    }

    private List<UiTarget> resolveTargets(UiContext context, List<TargetSelector> selectors) {
        if (selectors == null || selectors.isEmpty()) {
            return snapshot(context, SnapshotOptions.DEFAULT).targets();
        }
        return selectors.stream()
                .flatMap(selector -> query(context, selector).stream())
                .distinct()
                .toList();
    }

    private Map<String, Object> selectorToMap(TargetSelector selector) {
        if (selector == null) {
            return Map.of();
        }
        var result = new java.util.LinkedHashMap<String, Object>();
        if (selector.scope() != null) {
            result.put("scope", selector.scope());
        }
        if (selector.screen() != null) {
            result.put("screen", selector.screen());
        }
        if (selector.modId() != null) {
            result.put("modId", selector.modId());
        }
        if (selector.text() != null) {
            result.put("text", selector.text());
        }
        if (selector.role() != null) {
            result.put("role", selector.role());
        }
        if (selector.id() != null) {
            result.put("id", selector.id());
        }
        if (selector.index() != null) {
            result.put("index", selector.index());
        }
        if (selector.bounds() != null) {
            result.put("bounds", Map.of(
                    "x", selector.bounds().x(),
                    "y", selector.bounds().y(),
                    "width", selector.bounds().width(),
                    "height", selector.bounds().height()
            ));
        }
        return Map.copyOf(result);
    }

    private Map<String, Object> targetToMap(UiTarget target) {
        return Map.of(
                "targetId", target.targetId(),
                "role", target.role(),
                "bounds", Map.of(
                        "x", target.bounds().x(),
                        "y", target.bounds().y(),
                        "width", target.bounds().width(),
                        "height", target.bounds().height()
                )
        );
    }

    private UiTarget findTarget(List<UiTarget> targets, String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return null;
        }
        return targets.stream()
                .filter(target -> target.targetId().equals(targetId))
                .findFirst()
                .orElse(null);
    }

    private String effectiveTargetId(String sessionValue, String defaultValue) {
        return sessionValue != null ? sessionValue : defaultValue;
    }
}
