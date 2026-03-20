package dev.vfyjxf.moddev.api.registrar;

import dev.vfyjxf.moddev.api.event.RegisterCommonOperationsEvent;

/**
 * Callback interface implemented by common-side operation registrars discovered by ModDevMCP.
 */
@FunctionalInterface
public interface CommonOperationRegistrar {

    /**
     * Contributes side-neutral operations through the supplied event.
     */
    void register(RegisterCommonOperationsEvent event);
}