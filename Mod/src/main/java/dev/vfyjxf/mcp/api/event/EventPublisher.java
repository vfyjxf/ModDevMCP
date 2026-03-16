package dev.vfyjxf.mcp.api.event;

import java.util.List;

/**
 * Publisher and query surface for runtime events emitted through ModDevMCP.
 */
public interface EventPublisher {

    /**
     * Publishes a single event to the active runtime event stream.
     */
    void publish(EventEnvelope event);

    /**
     * Returns a bounded list of recently published events.
     */
    List<EventEnvelope> recentEvents();
}
