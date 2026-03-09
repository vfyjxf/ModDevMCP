package dev.vfyjxf.mcp.api.runtime;

import java.util.Map;

public interface UiContext {

    String screenClass();

    default String modId() {
        return "minecraft";
    }

    default int mouseX() {
        return 0;
    }

    default int mouseY() {
        return 0;
    }

    default Map<String, Object> attributes() {
        return Map.of();
    }
}
