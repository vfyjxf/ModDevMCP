package dev.vfyjxf.mcp.api.event;

import java.util.List;

public interface EventPublisher {

    void publish(EventEnvelope event);

    List<EventEnvelope> recentEvents();
}
