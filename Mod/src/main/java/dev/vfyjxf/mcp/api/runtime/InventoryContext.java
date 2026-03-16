package dev.vfyjxf.mcp.api.runtime;

import java.util.Map;

/**
 * Lightweight runtime context describing the active inventory or container.
 */
public interface InventoryContext {

    /**
     * Returns the logical container type identifier when known.
     */
    default String containerType() {
        return "unknown";
    }

    /**
     * Returns optional inventory-specific attributes associated with the context.
     */
    default Map<String, Object> attributes() {
        return Map.of();
    }
}
