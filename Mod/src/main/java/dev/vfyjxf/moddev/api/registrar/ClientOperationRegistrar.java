package dev.vfyjxf.moddev.api.registrar;

import dev.vfyjxf.moddev.api.event.RegisterClientOperationsEvent;

/**
 * Callback interface implemented by client-side operation registrars discovered by ModDevMCP.
 */
@FunctionalInterface
public interface ClientOperationRegistrar {

    /**
     * Contributes client-side operations or runtime adapters through the supplied event.
     */
    void register(RegisterClientOperationsEvent event);
}