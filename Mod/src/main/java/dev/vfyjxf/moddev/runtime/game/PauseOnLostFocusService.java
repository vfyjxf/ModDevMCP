package dev.vfyjxf.moddev.runtime.game;

public interface PauseOnLostFocusService {

    boolean currentState();

    boolean setEnabled(boolean enabled);
}

