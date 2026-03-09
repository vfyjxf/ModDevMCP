package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.ui.UiInteractionDefaults;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.List;

public interface UiInteractionStateResolver {

    String driverId();

    int priority();

    boolean matches(UiContext context, List<UiTarget> targets);

    UiInteractionDefaults resolve(UiContext context, List<UiTarget> targets);
}
