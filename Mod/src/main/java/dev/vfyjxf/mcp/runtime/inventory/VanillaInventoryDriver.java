package dev.vfyjxf.mcp.runtime.inventory;

import dev.vfyjxf.mcp.api.inventory.InventorySnapshot;
import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.DriverDescriptor;
import dev.vfyjxf.mcp.api.runtime.InventoryContext;
import dev.vfyjxf.mcp.api.runtime.InventoryDriver;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VanillaInventoryDriver implements InventoryDriver {

    private static final DriverDescriptor DESCRIPTOR = new DriverDescriptor(
            "vanilla-inventory",
            "minecraft",
            100,
            Set.of("snapshot", "action")
    );

    @Override
    public DriverDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean matches(InventoryContext context) {
        return true;
    }

    @Override
    public InventorySnapshot snapshot(InventoryContext context) {
        return new InventorySnapshot("container", context.containerType(), context.containerType(), List.of(), Map.of(), 0, Map.of("driverId", DESCRIPTOR.id()));
    }

    @Override
    public OperationResult<Map<String, Object>> action(InventoryContext context, String action, Map<String, Object> arguments) {
        return OperationResult.success(Map.of("driverId", DESCRIPTOR.id(), "action", action));
    }
}
