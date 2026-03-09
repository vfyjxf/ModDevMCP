package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.api.runtime.UiDriver;
import dev.vfyjxf.mcp.runtime.UiDriverRegistry;

public final class RegisterUiDriversEvent {

    private final UiDriverRegistry registry;

    public RegisterUiDriversEvent(UiDriverRegistry registry) {
        this.registry = registry;
    }

    public void register(UiDriver driver) {
        registry.register(driver);
    }
}
