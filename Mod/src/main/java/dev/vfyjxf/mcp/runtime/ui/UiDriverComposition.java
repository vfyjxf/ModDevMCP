package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiDriver;

import java.util.List;
import java.util.Objects;

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
