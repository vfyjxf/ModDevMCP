package dev.vfyjxf.mcp.api.runtime;

import java.util.Set;

public record DriverDescriptor(
        String id,
        String modId,
        int priority,
        Set<String> capabilities
) {
    public DriverDescriptor {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }
}
