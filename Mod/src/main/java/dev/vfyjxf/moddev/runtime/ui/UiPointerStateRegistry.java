package dev.vfyjxf.moddev.runtime.ui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UiPointerStateRegistry {

    private final Map<UiPointerKey, PointerState> states = new ConcurrentHashMap<>();

    public PointerState stateFor(String screenClass, String modId) {
        return states.getOrDefault(new UiPointerKey(screenClass, effectiveModId(modId)), PointerState.origin());
    }

    public void update(String screenClass, String modId, int mouseX, int mouseY) {
        if (screenClass == null || screenClass.isBlank()) {
            return;
        }
        states.put(new UiPointerKey(screenClass, effectiveModId(modId)), new PointerState(mouseX, mouseY));
    }

    private String effectiveModId(String modId) {
        return modId == null || modId.isBlank() ? "minecraft" : modId;
    }

    private record UiPointerKey(
            String screenClass,
            String modId
    ) {
    }

    public record PointerState(
            int mouseX,
            int mouseY
    ) {
        private static PointerState origin() {
            return new PointerState(0, 0);
        }
    }
}

