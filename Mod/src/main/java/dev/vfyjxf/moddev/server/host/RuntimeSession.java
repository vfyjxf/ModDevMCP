package dev.vfyjxf.moddev.server.host;

import java.util.List;
import java.util.Map;

public record RuntimeSession(
        String runtimeId,
        String runtimeSide,
        List<String> supportedScopes,
        List<String> supportedSides,
        Map<String, Object> state
) {
    public RuntimeSession {
        supportedScopes = List.copyOf(supportedScopes);
        supportedSides = List.copyOf(supportedSides);
        state = Map.copyOf(state);
    }
}


