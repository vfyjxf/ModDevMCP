package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.ui.UiInteractionDefaults;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.List;

/**
 * Strategy interface that derives default interaction state for a matching UI context.
 */
public interface UiInteractionStateResolver {

    /**
     * Returns the driver identifier this resolver is associated with.
     */
    String driverId();

    /**
     * Returns the selection priority for this resolver.
     */
    int priority();

    /**
     * Returns {@code true} when this resolver can derive defaults for the supplied context.
     */
    boolean matches(UiContext context, List<UiTarget> targets);

    /**
     * Produces default interaction state for the supplied targets and context.
     */
    UiInteractionDefaults resolve(UiContext context, List<UiTarget> targets);
}
