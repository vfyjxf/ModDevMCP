package dev.vfyjxf.moddev.api.event;

import dev.vfyjxf.moddev.api.ModMcpApi;
import dev.vfyjxf.moddev.api.operation.OperationExecutor;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.Objects;

/**
 * Base event passed to operation registrar callbacks while ModDevMCP is collecting operations.
 */
public abstract class RegisterOperationsEvent {

    private final ModMcpApi api;
    private final EventPublisher eventPublisher;

    protected RegisterOperationsEvent(ModMcpApi api, EventPublisher eventPublisher) {
        this.api = Objects.requireNonNull(api, "api");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    /**
     * Adds an operation definition/executor pair to the current registration batch.
     */
    public final void registerOperation(OperationDefinition definition, OperationExecutor executor) {
        api.registerOperation(definition, executor);
    }

    /**
     * Returns the public runtime registration API associated with this callback.
     */
    public final ModMcpApi api() {
        return api;
    }

    /**
     * Returns the shared event publisher associated with the current runtime.
     */
    public final EventPublisher eventPublisher() {
        return eventPublisher;
    }

    /**
     * Publishes a runtime event immediately through the shared event publisher.
     */
    public final void publishEvent(EventEnvelope event) {
        eventPublisher.publish(event);
    }
}