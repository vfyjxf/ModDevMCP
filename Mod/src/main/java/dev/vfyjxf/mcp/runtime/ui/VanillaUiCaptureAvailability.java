package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;

final class VanillaUiCaptureAvailability {

    private VanillaUiCaptureAvailability() {
    }

    static boolean hasMatchingLiveScreen(UiContext context) {
        try {
            var minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            var instance = minecraftClass.getMethod("getInstance").invoke(null);
            if (instance == null) {
                return false;
            }
            var screen = minecraftClass.getField("screen").get(instance);
            return screen != null && screen.getClass().getName().equals(context.screenClass());
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    static boolean hasLiveFramebuffer(UiContext context) {
        try {
            var minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            var instance = minecraftClass.getMethod("getInstance").invoke(null);
            if (instance == null) {
                return false;
            }
            return minecraftClass.getMethod("getWindow").invoke(instance) != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    static boolean supportsOffscreenSnapshot(UiSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (snapshot.screenClass() != null && snapshot.screenClass().startsWith("net.minecraft.client.gui.screens.worldselection.")) {
            return false;
        }
        return snapshot.targets().stream().noneMatch(VanillaUiCaptureAvailability::usesSelectionListRendering);
    }

    private static boolean usesSelectionListRendering(UiTarget target) {
        var widgetClass = target.extensions().get("widgetClass");
        if (!(widgetClass instanceof String className) || className.isBlank()) {
            return false;
        }
        return className.contains("SelectionList");
    }
}
