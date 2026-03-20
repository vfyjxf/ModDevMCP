package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.DriverDescriptor;
import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.ui.*;
import dev.vfyjxf.mcp.runtime.UiInteractionStateResolverRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VanillaContainerUiDriver extends VanillaScreenUiDriver {

    private static final DriverDescriptor DESCRIPTOR = new DriverDescriptor(
            "vanilla-container",
            "minecraft",
            200,
            Set.of("snapshot", "query", "capture", "inventory")
    );

    public VanillaContainerUiDriver() {
        super();
    }

    public VanillaContainerUiDriver(UiSessionStateRegistry sessionStates, UiInteractionStateResolverRegistry interactionResolvers) {
        super(sessionStates, interactionResolvers);
    }

    @Override
    public DriverDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean matches(UiContext context) {
        var screenClass = context.screenClass();
        if (screenClass == null || screenClass.isBlank()) {
            return false;
        }
        return screenClass.contains("Inventory")
                || screenClass.contains("Container")
                || screenClass.contains(".inventory.");
    }

    @Override
    public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
        var snapshot = super.snapshot(context, options);
        return new UiSnapshot(
                snapshot.screenId(),
                snapshot.screenClass(),
                DESCRIPTOR.id(),
                snapshot.targets(),
                snapshot.overlays(),
                snapshot.focusedTargetId(),
                snapshot.selectedTargetId(),
                snapshot.hoveredTargetId(),
                snapshot.activeTargetId(),
                Map.of("container", true)
        );
    }

    @Override
    public List<UiTarget> query(UiContext context, TargetSelector selector) {
        return snapshot(context, SnapshotOptions.DEFAULT).targets().stream()
                .filter(target -> matchesTarget(selector, target))
                .toList();
    }

    @Override
    public dev.vfyjxf.mcp.api.model.OperationResult<TooltipSnapshot> tooltip(UiContext context, TargetSelector selector) {
        var target = query(context, selector).stream().findFirst().orElse(snapshot(context, SnapshotOptions.DEFAULT).targets().getFirst());
        var lines = "slot".equals(target.role()) ? List.of("Slot 0") : List.of(target.text());
        return dev.vfyjxf.mcp.api.model.OperationResult.success(new TooltipSnapshot(
                target.targetId(),
                lines,
                target.bounds(),
                Map.of("driverId", DESCRIPTOR.id())
        ));
    }

    @Override
    protected List<UiTarget> baseTargets(UiContext context) {
        var root = new UiTarget(
                "container-root",
                DESCRIPTOR.id(),
                context.screenClass(),
                context.modId(),
                "screen",
                "Container",
                new Bounds(0, 0, 176, 166),
                UiTargetState.defaultState(),
                List.of("capture", "focus"),
                Map.of("container", true)
        );
        var slot = new UiTarget(
                "slot-0",
                DESCRIPTOR.id(),
                context.screenClass(),
                context.modId(),
                "slot",
                "",
                new Bounds(8, 18, 16, 16),
                UiTargetState.defaultState(),
                List.of("click", "hover"),
                Map.of("slotIndex", 0)
        );
        return List.of(root, slot);
    }
}
