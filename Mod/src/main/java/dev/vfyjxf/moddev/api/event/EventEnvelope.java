package dev.vfyjxf.moddev.api.event;

import java.util.Map;

/**
 * Immutable event payload published by runtime integrations and registrars.
 */
public record EventEnvelope(
        String domain,
        String type,
        long timestamp,
        Map<String, Object> payload
) {
    public EventEnvelope {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}

