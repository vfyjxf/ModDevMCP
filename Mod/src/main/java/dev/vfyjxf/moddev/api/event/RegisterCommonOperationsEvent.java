package dev.vfyjxf.moddev.api.event;

import dev.vfyjxf.moddev.api.ModMcpApi;

/**
 * Common-side registrar event used to register side-neutral operations.
 */
public final class RegisterCommonOperationsEvent extends RegisterOperationsEvent {

    public RegisterCommonOperationsEvent(ModMcpApi api, EventPublisher eventPublisher) {
        super(api, eventPublisher);
    }
}