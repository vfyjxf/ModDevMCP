package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.ui.*;
import dev.vfyjxf.mcp.runtime.ui.DefaultUiDriverSupport;

import java.util.List;
import java.util.Map;

/**
 * Adapter interface that teaches ModDevMCP how to understand and interact with a family of UI
 * screens.
 *
 * <p>A driver is selected by {@link #matches(UiContext)} and then used to produce snapshots,
 * resolve targets, perform actions, and report interaction state.
 */
public interface UiDriver {

    /**
     * Returns static metadata about the driver, including its identity and selection priority.
     */
    DriverDescriptor descriptor();

    /**
     * Returns {@code true} when this driver can handle the supplied UI context.
     */
    boolean matches(UiContext context);

    /**
     * Builds a complete snapshot of the current screen state for this driver.
     */
    UiSnapshot snapshot(UiContext context, SnapshotOptions options);

    /**
     * Returns the targets that match the supplied selector within this driver's current snapshot.
     */
    List<UiTarget> query(UiContext context, TargetSelector selector);

    /**
     * Captures an image or logical capture payload for the supplied request.
     */
    default OperationResult<Map<String, Object>> capture(UiContext context, CaptureRequest request) {
        return OperationResult.rejected("Capture not supported");
    }

    /**
     * Performs a driver-specific action against a matching target.
     */
    default OperationResult<Map<String, Object>> action(UiContext context, UiActionRequest request) {
        return OperationResult.rejected("Action not supported");
    }

    /**
     * Returns the targets under the supplied coordinates.
     */
    default OperationResult<List<UiTarget>> inspectAt(UiContext context, int x, int y) {
        return OperationResult.rejected("Inspect not supported");
    }

    /**
     * Produces tooltip content for the first matching target.
     */
    default OperationResult<TooltipSnapshot> tooltip(UiContext context, TargetSelector selector) {
        return OperationResult.rejected("Tooltip not supported");
    }

    /**
     * Reports the driver's current interaction state for the supplied context.
     */
    default UiInteractionState interactionState(UiContext context) {
        return new UiInteractionState(null, null, null, null, context.mouseX(), context.mouseY(), false, "unknown", descriptor().id());
    }

    /**
     * Resolves a target reference or locator into concrete targets using the default resolver.
     */
    default UiResolveResult resolve(UiContext context, UiResolveRequest request) {
        return DefaultUiDriverSupport.resolve(this, context, request);
    }

    /**
     * Checks whether a target is actionable for the requested action.
     */
    default UiActionabilityResult checkActionability(UiContext context, UiTarget target, String action) {
        return DefaultUiDriverSupport.checkActionability(target, action);
    }

    /**
     * Waits for a driver-level condition to match.
     */
    default UiWaitResult waitFor(UiContext context, UiWaitRequest request) {
        return DefaultUiDriverSupport.waitFor(this, context, request);
    }

    /**
     * Runs a higher-level driver intent when the driver exposes one.
     */
    default OperationResult<Map<String, Object>> runIntent(UiContext context, String intent, Map<String, Object> arguments) {
        return OperationResult.rejected("unsupported_intent");
    }

    /**
     * Produces a concise inspection payload using the default inspection helper.
     */
    default UiInspectResult inspect(UiContext context, SnapshotOptions options) {
        return DefaultUiDriverSupport.inspect(this, context, options);
    }
}
