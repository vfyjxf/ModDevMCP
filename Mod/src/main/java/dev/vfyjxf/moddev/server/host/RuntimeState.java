package dev.vfyjxf.moddev.server.host;

import java.util.Collection;

public record RuntimeState(
        boolean gameConnected,
        boolean gameConnecting,
        String runtimeId,
        String runtimeSide
) {
    public static RuntimeState disconnected() {
        return new RuntimeState(false, false, "", "");
    }

    public static RuntimeState fromSessions(Collection<RuntimeSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return disconnected();
        }
        if (sessions.size() == 1) {
            var session = sessions.iterator().next();
            return new RuntimeState(true, false, session.runtimeId(), session.runtimeSide());
        }
        return new RuntimeState(true, false, "", "");
    }
}

