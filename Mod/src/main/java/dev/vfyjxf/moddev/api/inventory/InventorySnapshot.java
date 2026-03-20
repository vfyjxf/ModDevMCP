package dev.vfyjxf.moddev.api.inventory;

import java.util.List;
import java.util.Map;

public record InventorySnapshot(
        String containerId,
        String containerType,
        String title,
        List<Map<String, Object>> slots,
        Map<String, Object> cursorStack,
        Integer selectedHotbarSlot,
        Map<String, Object> extensions
) {
    public InventorySnapshot {
        slots = slots == null ? List.of() : List.copyOf(slots);
        cursorStack = cursorStack == null ? Map.of() : Map.copyOf(cursorStack);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}

