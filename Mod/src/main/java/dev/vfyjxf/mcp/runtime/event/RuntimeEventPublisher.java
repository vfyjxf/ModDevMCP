package dev.vfyjxf.mcp.runtime.event;

import dev.vfyjxf.mcp.api.event.EventEnvelope;
import dev.vfyjxf.mcp.api.event.EventPublisher;

import java.util.ArrayList;
import java.util.List;

public final class RuntimeEventPublisher implements EventPublisher {

    private final List<EventEnvelope> events = new ArrayList<>();

    @Override
    public void publish(EventEnvelope event) {
        events.add(event);
    }

    @Override
    public List<EventEnvelope> recentEvents() {
        return List.copyOf(events);
    }
}
