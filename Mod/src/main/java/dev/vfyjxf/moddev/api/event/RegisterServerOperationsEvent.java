package dev.vfyjxf.moddev.api.event;

import dev.vfyjxf.moddev.api.ModMcpApi;

/**
 * Server-side registrar event used to register server runtime operations.
 */
public final class RegisterServerOperationsEvent extends RegisterOperationsEvent {

    public RegisterServerOperationsEvent(ModMcpApi api, EventPublisher eventPublisher) {
        super(api, eventPublisher);
    }
}