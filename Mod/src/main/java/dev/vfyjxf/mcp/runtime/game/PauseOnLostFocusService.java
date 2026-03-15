package dev.vfyjxf.mcp.runtime.game;

public interface PauseOnLostFocusService {

    boolean currentState();

    boolean setEnabled(boolean enabled);
}
