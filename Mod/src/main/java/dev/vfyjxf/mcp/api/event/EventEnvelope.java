package dev.vfyjxf.mcp.api.event;

import java.util.Map;

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
