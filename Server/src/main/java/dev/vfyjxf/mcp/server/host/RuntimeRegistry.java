package dev.vfyjxf.mcp.server.host;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RuntimeRegistry {

    private RuntimeState state = RuntimeState.disconnected();
    private RuntimeSession activeSession;
    private List<RuntimeToolDescriptor> dynamicTools = List.of();

    public synchronized RuntimeState state() {
        return state;
    }

    public synchronized Optional<RuntimeSession> activeSession() {
        return Optional.ofNullable(activeSession);
    }

    public synchronized List<RuntimeToolDescriptor> listDynamicTools() {
        return dynamicTools;
    }

    public synchronized void connect(RuntimeSession session, List<RuntimeToolDescriptor> tools) {
        activeSession = Objects.requireNonNull(session, "session");
        dynamicTools = List.copyOf(tools);
        state = RuntimeState.connected(session);
    }

    public synchronized void refreshTools(String runtimeId, List<RuntimeToolDescriptor> tools) {
        if (activeSession == null || !Objects.equals(activeSession.runtimeId(), runtimeId)) {
            return;
        }
        dynamicTools = List.copyOf(tools);
    }

    public synchronized void disconnect(String runtimeId) {
        if (activeSession == null || !Objects.equals(activeSession.runtimeId(), runtimeId)) {
            return;
        }
        activeSession = null;
        dynamicTools = List.of();
        state = RuntimeState.disconnected();
    }
}

