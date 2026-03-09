package dev.vfyjxf.mcp.api.runtime;

import java.util.Map;

public interface InventoryContext {

    default String containerType() {
        return "unknown";
    }

    default Map<String, Object> attributes() {
        return Map.of();
    }
}
