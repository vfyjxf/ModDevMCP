package dev.vfyjxf.moddev.api.registrar;

import dev.vfyjxf.moddev.api.event.RegisterServerOperationsEvent;

/**
 * Callback interface implemented by server-side operation registrars discovered by ModDevMCP.
 */
@FunctionalInterface
public interface ServerOperationRegistrar {

    /**
     * Contributes server-side operations through the supplied event.
     */
    void register(RegisterServerOperationsEvent event);
}