package dev.vfyjxf.mcp.api.event;

import dev.vfyjxf.mcp.api.runtime.InventoryDriver;
import dev.vfyjxf.mcp.runtime.InventoryDriverRegistry;

public final class RegisterInventoryDriversEvent {

    private final InventoryDriverRegistry registry;

    public RegisterInventoryDriversEvent(InventoryDriverRegistry registry) {
        this.registry = registry;
    }

    public void register(InventoryDriver driver) {
        registry.register(driver);
    }
}
