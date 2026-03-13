package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.DriverDescriptor;
import dev.vfyjxf.mcp.api.runtime.UiInspectResult;
import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiDriver;
import dev.vfyjxf.mcp.api.runtime.UiLocator;
import dev.vfyjxf.mcp.api.runtime.UiResolveRequest;
import dev.vfyjxf.mcp.api.runtime.UiResolveResult;
import dev.vfyjxf.mcp.api.runtime.UiTargetReference;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class VanillaScreenUiDriver implements UiDriver {

    private static final DriverDescriptor DESCRIPTOR = new DriverDescriptor(
            "vanilla-screen",
            "minecraft",
            100,
            Set.of("snapshot", "query", "capture", "action")
    );
    private static final long EXECUTION_TIMEOUT_SECONDS = 5L;
    private final UiSessionStateRegistry sessionStates;
    private final UiInteractionStateResolverRegistry interactionResolvers;
    private final UiTargetExtractor targetExtractor;
    private final UiActionExecutor actionExecutor;

    public VanillaScreenUiDriver() {
        this(new UiSessionStateRegistry(), BuiltinUiInteractionResolvers.newRegistry(), new LiveUiTargetExtractor(), null);
    }

    public VanillaScreenUiDriver(UiSessionStateRegistry sessionStates, UiInteractionStateResolverRegistry interactionResolvers) {
        this(sessionStates, interactionResolvers, new LiveUiTargetExtractor(), null);
    }

    VanillaScreenUiDriver(
            UiSessionStateRegistry sessionStates,
            UiInteractionStateResolverRegistry interactionResolvers,
            UiTargetExtractor targetExtractor
    ) {
        this(sessionStates, interactionResolvers, targetExtractor, null);
    }

    VanillaScreenUiDriver(
            UiSessionStateRegistry sessionStates,
            UiInteractionStateResolverRegistry interactionResolvers,
            UiTargetExtractor targetExtractor,
            UiActionExecutor actionExecutor
    ) {
        this.sessionStates = sessionStates;
        this.interactionResolvers = interactionResolvers;
        this.targetExtractor = targetExtractor;
        this.actionExecutor = actionExecutor == null ? new LiveUiActionExecutor() : actionExecutor;
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
        var focusedTargetId = effectiveFocusedTargetId(sessionState, defaults);
        var selectedTargetId = effectiveSelectedTargetId(sessionState, defaults);
        var hoveredTargetId = effectiveHoveredTargetId(sessionState, defaults, baseTargets, context);
        var activeTargetId = effectiveActiveTargetId(sessionState, defaults);
        var targets = applySessionState(baseTargets, focusedTargetId, selectedTargetId, hoveredTargetId, activeTargetId);
        return new UiSnapshot("screen", context.screenClass(), descriptor().id(), targets, List.of(),
                focusedTargetId,
                selectedTargetId,
                hoveredTargetId,
                activeTargetId,
                Map.of());
    }

    @Override
    public List<UiTarget> query(UiContext context, TargetSelector selector) {
        return snapshot(context, SnapshotOptions.DEFAULT).targets().stream()
                .filter(target -> matchesTarget(selector, target))
                .toList();
    }

    @Override
    public UiResolveResult resolve(UiContext context, UiResolveRequest request) {
        if (request.reference() == null) {
            return UiDriver.super.resolve(context, request);
        }
        var reference = request.reference();
        if (reference.ref() != null || (reference.pointX() != null && reference.pointY() != null)) {
            return UiDriver.super.resolve(context, request);
        }
        var locator = reference.locator();
        if (locator == null) {
            return UiDriver.super.resolve(context, request);
        }
        var matches = snapshot(context, SnapshotOptions.DEFAULT).targets().stream()
                .filter(target -> !"screen".equals(target.role()))
                .filter(target -> matchesLocator(target, locator))
                .filter(target -> request.includeHidden() || target.state().visible())
                .filter(target -> request.includeDisabled() || target.state().enabled())
                .toList();
        if (matches.isEmpty()) {
            return new UiResolveResult("not_found", List.of(), null, "target_not_found", Map.of());
        }
        if (locator.index() != null) {
            if (locator.index() < 0 || locator.index() >= matches.size()) {
                return new UiResolveResult("not_found", List.of(), null, "target_not_found", Map.of(
                        "index", locator.index()
                ));
            }
            var indexed = matches.get(locator.index());
            return new UiResolveResult("resolved", List.of(indexed), indexed, null, Map.of());
        }
        if (!request.allowMultiple() && matches.size() > 1) {
            return new UiResolveResult("ambiguous", matches, null, "target_ambiguous", Map.of(
                    "matchCount", matches.size()
            ));
        }
        return new UiResolveResult("resolved", matches, matches.getFirst(), null, Map.of());
    }

    @Override
    public UiInspectResult inspect(UiContext context, SnapshotOptions options) {
        var snapshot = snapshot(context, options);
        var interactionState = interactionState(context);
        var visibleTargets = snapshot.targets().stream()
                .filter(target -> !"screen".equals(target.role()))
                .toList();
        var actionableCount = (int) visibleTargets.stream()
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
                        "targetCount", visibleTargets.size(),
                        "actionableCount", actionableCount
                ),
                visibleTargets,
                interaction,
                null
        );
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

    protected boolean matchesLocator(UiTarget target, UiLocator locator) {
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
        if (locator.scopeRef() != null && !locator.scopeRef().isBlank() && !"screen-root".equals(locator.scopeRef())) {
            var scopeRef = target.extensions().get("scopeRef");
            if (!locator.scopeRef().equals(scopeRef)) {
                return false;
            }
        }
        return true;
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
        if ("hover".equals(request.action()) || "click".equals(request.action())) {
            var target = query(context, request.target()).stream().findFirst().orElse(null);
            if (target == null) {
                return OperationResult.rejected("target_not_found");
            }
            var actionResult = actionExecutor.execute(context, target, request);
            if (!actionResult.accepted()) {
                return actionResult;
            }
            var updatedState = "hover".equals(request.action())
                    ? sessionStates.stateFor(context, descriptor().id()).withHover(target.targetId(), "programmatic")
                    : sessionStates.stateFor(context, descriptor().id()).withActive(target.targetId(), "programmatic");
            sessionStates.update(context, descriptor().id(), updatedState);
            return actionResult;
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
        var targets = new ArrayList<UiTarget>();
        targets.add(new UiTarget(
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
        targets.addAll(targetExtractor.extract(context));
        return List.copyOf(targets);
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

    private String effectiveHoveredTargetId(
            UiSessionState state,
            UiInteractionDefaults defaults,
            List<UiTarget> targets,
            UiContext context
    ) {
        if (state.hoveredTargetId() != null) {
            return state.hoveredTargetId();
        }
        if (defaults.hoveredTargetId() != null) {
            return defaults.hoveredTargetId();
        }
        return inferHoveredTargetId(targets, context.mouseX(), context.mouseY());
    }

    private String effectiveActiveTargetId(UiSessionState state, UiInteractionDefaults defaults) {
        return state.activeTargetId() != null ? state.activeTargetId() : defaults.activeTargetId();
    }

    private String inferHoveredTargetId(List<UiTarget> targets, int mouseX, int mouseY) {
        return targets.stream()
                .filter(target -> !"screen".equals(target.role()))
                .filter(target -> contains(target, mouseX, mouseY))
                .min((left, right) -> Integer.compare(area(left), area(right)))
                .map(UiTarget::targetId)
                .orElse(null);
    }

    private int area(UiTarget target) {
        return target.bounds().width() * target.bounds().height();
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    @FunctionalInterface
    interface UiTargetExtractor {
        List<UiTarget> extract(UiContext context);
    }

    @FunctionalInterface
    interface UiActionExecutor {
        OperationResult<Map<String, Object>> execute(UiContext context, UiTarget target, UiActionRequest request);
    }

    private final class LiveUiActionExecutor implements UiActionExecutor {

        @Override
        public OperationResult<Map<String, Object>> execute(UiContext context, UiTarget target, UiActionRequest request) {
            try {
                var minecraft = Minecraft.getInstance();
                if (minecraft.isSameThread()) {
                    return executeOnClientThread(target, request);
                }
                var future = new CompletableFuture<OperationResult<Map<String, Object>>>();
                minecraft.execute(() -> future.complete(executeOnClientThread(target, request)));
                try {
                    return future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return OperationResult.rejected("interrupted while waiting for ui action execution");
                } catch (ExecutionException exception) {
                    return OperationResult.rejected("ui action execution failed: " + exception.getCause().getMessage());
                } catch (TimeoutException exception) {
                    return OperationResult.rejected("timed out waiting for ui action execution");
                }
            } catch (NoClassDefFoundError exception) {
                return OperationResult.success(Map.of(
                        "driverId", descriptor().id(),
                        "action", request.action(),
                        "performed", true,
                        "targetId", target.targetId()
                ));
            }
        }

        private OperationResult<Map<String, Object>> executeOnClientThread(UiTarget target, UiActionRequest request) {
            var screen = Minecraft.getInstance().screen;
            if (screen == null) {
                return OperationResult.rejected("game_unavailable: no active client screen");
            }
            var centerX = target.bounds().x() + (target.bounds().width() / 2.0d);
            var centerY = target.bounds().y() + (target.bounds().height() / 2.0d);
            return switch (request.action()) {
                case "hover" -> hover(screen, target, centerX, centerY, request);
                case "click" -> click(screen, target, centerX, centerY, request);
                default -> OperationResult.rejected("unsupported_action");
            };
        }

        private OperationResult<Map<String, Object>> hover(Screen screen, UiTarget target, double x, double y, UiActionRequest request) {
            screen.mouseMoved(x, y);
            if (request.arguments().get("hoverDelayMs") instanceof Number hoverDelayMs && hoverDelayMs.intValue() > 0) {
                try {
                    Thread.sleep(hoverDelayMs.intValue());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return OperationResult.rejected("interrupted while waiting for hover delay");
                }
            }
            return OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true,
                    "targetId", target.targetId()
            ));
        }

        private OperationResult<Map<String, Object>> click(Screen screen, UiTarget target, double x, double y, UiActionRequest request) {
            var minecraft = Minecraft.getInstance();
            if (PauseScreenBackToGameSupport.resumeIfPauseBackTarget(
                    screen.getClass().getName(),
                    target,
                    screen::onClose,
                    minecraft.mouseHandler::grabMouse
            )) {
                return OperationResult.success(Map.of(
                        "driverId", descriptor().id(),
                        "action", request.action(),
                        "performed", true,
                        "targetId", target.targetId()
                ));
            }
            if (VanillaWidgetPressSupport.invokeButtonPress(screen, target)) {
                return OperationResult.success(Map.of(
                        "driverId", descriptor().id(),
                        "action", request.action(),
                        "performed", true,
                        "targetId", target.targetId()
                ));
            }
            screen.mouseMoved(x, y);
            var button = request.arguments().get("button") instanceof Number number ? number.intValue() : 0;
            var clicked = screen.mouseClicked(x, y, button);
            var released = screen.mouseReleased(x, y, button);
            if (!clicked && !released) {
                return OperationResult.rejected("click was not handled by current screen");
            }
            return OperationResult.success(Map.of(
                    "driverId", descriptor().id(),
                    "action", request.action(),
                    "performed", true,
                    "targetId", target.targetId()
            ));
        }
    }

    private static final class LiveUiTargetExtractor implements UiTargetExtractor {

        @Override
        public List<UiTarget> extract(UiContext context) {
            var screen = liveScreen(context.screenClass());
            if (screen == null) {
                return List.of();
            }
            return VanillaWidgetIntrospection.extractTargets(screen, context, "vanilla-screen");
        }

        private Object liveScreen(String expectedScreenClass) {
            try {
                var minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                var getInstance = minecraftClass.getMethod("getInstance");
                var minecraft = getInstance.invoke(null);
                var screenField = minecraftClass.getField("screen");
                var screen = screenField.get(minecraft);
                if (screen == null || !Objects.equals(screen.getClass().getName(), expectedScreenClass)) {
                    return null;
                }
                return screen;
            } catch (ReflectiveOperationException | LinkageError exception) {
                return null;
            }
        }
    }
}
