package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UiSessionStateRegistry {

    private final Map<UiSessionKey, UiSessionState> states = new ConcurrentHashMap<>();

    public UiSessionState stateFor(UiContext context, String driverId) {
        return states.getOrDefault(new UiSessionKey(driverId, context.screenClass(), context.modId()), UiSessionState.openedState());
    }

    public void update(UiContext context, String driverId, UiSessionState state) {
        states.put(new UiSessionKey(driverId, context.screenClass(), context.modId()), state);
    }

    private record UiSessionKey(
            String driverId,
            String screenClass,
            String modId
    ) {
    }
}
