package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.SnapshotOptions;
import dev.vfyjxf.mcp.api.ui.TargetSelector;
import dev.vfyjxf.mcp.api.ui.TooltipSnapshot;
import dev.vfyjxf.mcp.api.ui.UiActionRequest;
import dev.vfyjxf.mcp.api.ui.UiInteractionState;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.List;
import java.util.Map;

public interface UiDriver {

    DriverDescriptor descriptor();

    boolean matches(UiContext context);

    UiSnapshot snapshot(UiContext context, SnapshotOptions options);

    List<UiTarget> query(UiContext context, TargetSelector selector);

    default OperationResult<Map<String, Object>> capture(UiContext context, CaptureRequest request) {
        return OperationResult.rejected("Capture not supported");
    }

    default OperationResult<Map<String, Object>> action(UiContext context, UiActionRequest request) {
        return OperationResult.rejected("Action not supported");
    }

    default OperationResult<List<UiTarget>> inspectAt(UiContext context, int x, int y) {
        return OperationResult.rejected("Inspect not supported");
    }

    default OperationResult<TooltipSnapshot> tooltip(UiContext context, TargetSelector selector) {
        return OperationResult.rejected("Tooltip not supported");
    }

    default UiInteractionState interactionState(UiContext context) {
        return new UiInteractionState(null, null, null, null, context.mouseX(), context.mouseY(), false, "unknown", descriptor().id());
    }
}
