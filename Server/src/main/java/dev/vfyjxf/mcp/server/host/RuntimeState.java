package dev.vfyjxf.mcp.server.host;

public record RuntimeState(
        boolean gameConnected,
        boolean gameConnecting,
        String runtimeId,
        String runtimeSide
) {
    public static RuntimeState disconnected() {
        return new RuntimeState(false, false, "", "");
    }

    public static RuntimeState connected(RuntimeSession session) {
        return new RuntimeState(true, false, session.runtimeId(), session.runtimeSide());
    }
}

