package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.inventory.InventorySnapshot;
import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.ui.TargetSelector;

import java.util.List;
import java.util.Map;

public interface InventoryDriver {

    DriverDescriptor descriptor();

    boolean matches(InventoryContext context);

    InventorySnapshot snapshot(InventoryContext context);

    default List<Map<String, Object>> querySlots(InventoryContext context, TargetSelector selector) {
        return List.of();
    }

    default OperationResult<Map<String, Object>> action(InventoryContext context, String action, Map<String, Object> arguments) {
        return OperationResult.rejected("Inventory action not supported");
    }
}
