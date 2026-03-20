package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiDriver;

import java.util.List;
import java.util.Objects;

/**
 * Immutable view of the drivers currently participating in one live UI context.
 *
 * <p>The composition keeps the original {@link UiContext}, the ordered active drivers, and the
 * default driver id that higher-level tools use when they still need a single-driver fallback.
 */
public record UiDriverComposition(
        UiContext context,
        List<UiDriver> drivers,
        String defaultDriverId
) {
    public UiDriverComposition {
        context = Objects.requireNonNull(context, "context");
        drivers = drivers == null ? List.of() : List.copyOf(drivers);
        defaultDriverId = defaultDriverId == null ? "" : defaultDriverId;
    }
}
