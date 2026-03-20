package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.runtime.UiDriverRegistry;

import java.util.Objects;

/**
 * Resolves the ordered set of drivers that match a live {@link UiContext}.
 *
 * <p>The first matching driver remains the default driver so existing single-driver flows can
 * degrade gracefully while newer tooling works with the full composition.
 */
public final class UiDriverCompositionResolver {

    private final UiDriverRegistry registry;

    public UiDriverCompositionResolver(UiDriverRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Returns all active drivers for the supplied context in registry priority order.
     */
    public UiDriverComposition resolve(UiContext context) {
        var drivers = registry.matchingDrivers(context);
        var defaultDriverId = drivers.isEmpty() ? "" : drivers.getFirst().descriptor().id();
        return new UiDriverComposition(context, drivers, defaultDriverId);
    }
}
