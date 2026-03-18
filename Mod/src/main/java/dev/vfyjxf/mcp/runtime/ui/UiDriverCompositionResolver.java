package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.runtime.UiDriverRegistry;

import java.util.Objects;

public final class UiDriverCompositionResolver {

    private final UiDriverRegistry registry;

    public UiDriverCompositionResolver(UiDriverRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public UiDriverComposition resolve(UiContext context) {
        var drivers = registry.matchingDrivers(context);
        var defaultDriverId = drivers.isEmpty() ? "" : drivers.getFirst().descriptor().id();
        return new UiDriverComposition(context, drivers, defaultDriverId);
    }
}
