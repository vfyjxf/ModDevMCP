package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.DriverDescriptor;
import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiDriver;
import dev.vfyjxf.mcp.api.ui.UiInteractionDefaults;
import dev.vfyjxf.mcp.api.ui.Bounds;
import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.SnapshotOptions;
import dev.vfyjxf.mcp.api.ui.TargetSelector;
import dev.vfyjxf.mcp.api.ui.TooltipSnapshot;
import dev.vfyjxf.mcp.api.ui.UiActionRequest;
import dev.vfyjxf.mcp.api.ui.UiInteractionState;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;
import dev.vfyjxf.mcp.api.ui.UiTargetState;
import dev.vfyjxf.mcp.runtime.UiInteractionStateResolverRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class VanillaScreenUiDriver implements UiDriver {

    private static final DriverDescriptor DESCRIPTOR = new DriverDescriptor(
            "vanilla-screen",
            "minecraft",
            100,
            Set.of("snapshot", "query", "capture")
    );
    private final UiSessionStateRegistry sessionStates;
    private final UiInteractionStateResolverRegistry interactionResolvers;

    public VanillaScreenUiDriver() {
        this(new UiSessionStateRegistry(), BuiltinUiInteractionResolvers.newRegistry());
    }

    public VanillaScreenUiDriver(UiSessionStateRegistry sessionStates, UiInteractionStateResolverRegistry interactionResolvers) {
        this.sessionStates = sessionStates;
        this.interactionResolvers = interactionResolvers;
    }

    @Override
    public DriverDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean matches(UiContext context) {
        return context.screenClass().startsWith("net.minecraft.client.gui.screens.")
                && !context.screenClass().contains(".inventory.");
    }

    @Override
    public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
        var sessionState = sessionStates.stateFor(context, descriptor().id());
        if (!sessionState.open()) {
            return new UiSnapshot("screen", context.screenClass(), descriptor().id(), List.of(), List.of(), null, null, null, null, Map.of("closed", true));
        }
        var baseTargets = baseTargets(context);
        var defaults = interactionResolvers.resolve(descriptor().id(), context, baseTargets);
        var targets = applySessionState(baseTargets, effectiveFocusedTargetId(sessionState, defaults), effectiveSelectedTargetId(sessionState, defaults),
                effectiveHoveredTargetId(sessionState, defaults), effectiveActiveTargetId(sessionState, defaults));
        return new UiSnapshot("screen", context.screenClass(), descriptor().id(), targets, List.of(),
                effectiveFocusedTargetId(sessionState, defaults),
                effectiveSelectedTargetId(sessionState, defaults),
                effectiveHoveredTargetId(sessionState, defaults),
                effectiveActiveTargetId(sessionState, defaults),
                Map.of());
    }

    @Override
    public List<UiTarget> query(UiContext context, TargetSelector selector) {
        return snapshot(context, SnapshotOptions.DEFAULT).targets().stream()
                .filter(target -> matchesTarget(selector, target))
                .toList();
    }

    protected boolean matchesTarget(TargetSelector selector, UiTarget target) {
        return matchesScope(selector, target)
                && (selector.role() == null || selector.role().equals(target.role()))
                && (selector.id() == null || selector.id().equals(target.targetId()))
                && (selector.text() == null || selector.text().equals(target.text()))
                && (selector.modId() == null || selector.modId().equals(target.modId()))
                && intersects(selector, target);
    }

    protected boolean matchesScope(TargetSelector selector, UiTarget target) {
        if (selector.scope() == null || selector.scope().isBlank() || "element".equals(selector.scope())) {
            return true;
        }
        return selector.scope().equals(target.role());
    }

    protected boolean intersects(TargetSelector selector, UiTarget target) {
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
                "mode", request.mode(),
                "driverId", descriptor().id(),
                "capturedTargets", filtered.stream().map(this::targetToMap).toList(),
                "excludedTargets", excluded.stream().map(this::targetToMap).toList()
        ));
    }

    @Override
    public OperationResult<Map<String, Object>> action(UiContext context, UiActionRequest request) {
        if ("close".equals(request.action())) {
            sessionStates.update(context, descriptor().id(), UiSessionState.closedState());
            return OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true
            ));
        }
        if ("open".equals(request.action())) {
            sessionStates.update(context, descriptor().id(), UiSessionState.openedState().reopened("programmatic"));
            return OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true
            ));
        }
        if ("switch".equals(request.action())) {
            var target = query(context, request.target()).stream().findFirst().orElse(baseTargets(context).getFirst());
            sessionStates.update(context, descriptor().id(), UiSessionState.openedState().withFocus(target.targetId(), "programmatic"));
            return OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true,
                    "targetId", target.targetId()
            ));
        }
        return OperationResult.success(Map.of(
                "driverId", descriptor().id(),
                "action", request.action(),
                "performed", true
        ));
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
                List.of(target.text().isBlank() ? target.role() : target.text()),
                target.bounds(),
                Map.of("driverId", descriptor().id())
        ));
    }

    protected boolean contains(UiTarget target, int x, int y) {
        var bounds = target.bounds();
        return x >= bounds.x() && x < bounds.x() + bounds.width()
                && y >= bounds.y() && y < bounds.y() + bounds.height();
    }

    @Override
    public UiInteractionState interactionState(UiContext context) {
        var sessionState = sessionStates.stateFor(context, descriptor().id());
        if (!sessionState.open()) {
            return new UiInteractionState(null, null, null, null, context.mouseX(), context.mouseY(), false, "closed", descriptor().id());
        }
        var snapshot = snapshot(context, SnapshotOptions.DEFAULT);
        var defaults = interactionResolvers.resolve(descriptor().id(), context, snapshot.targets());
        return new UiInteractionState(
                findTarget(snapshot.targets(), snapshot.focusedTargetId()),
                findTarget(snapshot.targets(), snapshot.selectedTargetId()),
                findTarget(snapshot.targets(), snapshot.hoveredTargetId()),
                findTarget(snapshot.targets(), snapshot.activeTargetId()),
                context.mouseX(),
                context.mouseY(),
                false,
                "unknown".equals(sessionState.selectionSource()) ? defaults.selectionSource() : sessionState.selectionSource(),
                descriptor().id()
        );
    }

    protected List<UiTarget> resolveTargets(UiContext context, List<TargetSelector> selectors) {
        if (selectors == null || selectors.isEmpty()) {
            return snapshot(context, SnapshotOptions.DEFAULT).targets();
        }
        return selectors.stream()
                .flatMap(selector -> query(context, selector).stream())
                .distinct()
                .toList();
    }

    protected Map<String, Object> targetToMap(UiTarget target) {
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

    protected List<UiTarget> baseTargets(UiContext context) {
        return List.of(new UiTarget(
                "screen-root",
                descriptor().id(),
                context.screenClass(),
                context.modId(),
                "screen",
                context.screenClass(),
                new Bounds(0, 0, 320, 240),
                UiTargetState.defaultState(),
                List.of("capture", "focus"),
                Map.of()
        ));
    }

    protected List<UiTarget> applySessionState(
            List<UiTarget> targets,
            String focusedTargetId,
            String selectedTargetId,
            String hoveredTargetId,
            String activeTargetId
    ) {
        return targets.stream()
                .map(target -> new UiTarget(
                        target.targetId(),
                        target.driverId(),
                        target.screenClass(),
                        target.modId(),
                        target.role(),
                        target.text(),
                        target.bounds(),
                        new UiTargetState(
                                target.state().visible(),
                                target.state().enabled(),
                                target.targetId().equals(focusedTargetId),
                                target.targetId().equals(hoveredTargetId),
                                target.targetId().equals(selectedTargetId),
                                target.targetId().equals(activeTargetId)
                        ),
                        target.actions(),
                        target.extensions()
                ))
                .toList();
    }

    protected UiTarget findTarget(List<UiTarget> targets, String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return null;
        }
        return targets.stream()
                .filter(target -> target.targetId().equals(targetId))
                .findFirst()
                .orElse(null);
    }

    private String effectiveFocusedTargetId(UiSessionState state, UiInteractionDefaults defaults) {
        return state.focusedTargetId() != null ? state.focusedTargetId() : defaults.focusedTargetId();
    }

    private String effectiveSelectedTargetId(UiSessionState state, UiInteractionDefaults defaults) {
        return state.selectedTargetId() != null ? state.selectedTargetId() : defaults.selectedTargetId();
    }

    private String effectiveHoveredTargetId(UiSessionState state, UiInteractionDefaults defaults) {
        return state.hoveredTargetId() != null ? state.hoveredTargetId() : defaults.hoveredTargetId();
    }

    private String effectiveActiveTargetId(UiSessionState state, UiInteractionDefaults defaults) {
        return state.activeTargetId() != null ? state.activeTargetId() : defaults.activeTargetId();
    }
}
