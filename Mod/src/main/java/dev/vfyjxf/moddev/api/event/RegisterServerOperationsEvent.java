package dev.vfyjxf.moddev.api.event;

import dev.vfyjxf.moddev.api.ModDevApi;

/**
 * Server-side registrar event used to register server runtime operations.
 */
public final class RegisterServerOperationsEvent extends RegisterOperationsEvent {

    public RegisterServerOperationsEvent(ModDevApi api, EventPublisher eventPublisher) {
        super(api, eventPublisher);
    }
}