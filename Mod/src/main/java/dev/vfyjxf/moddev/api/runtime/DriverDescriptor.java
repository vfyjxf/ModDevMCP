package dev.vfyjxf.moddev.api.runtime;

import java.util.Set;

/**
 * Static metadata describing a runtime adapter or UI driver.
 */
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

