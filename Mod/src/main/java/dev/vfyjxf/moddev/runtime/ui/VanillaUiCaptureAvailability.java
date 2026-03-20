package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;

final class VanillaUiCaptureAvailability {

    private static final String PAUSE_SCREEN_CLASS = "net.minecraft.client.gui.screens.PauseScreen";

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

    static boolean supportsOffscreenSnapshot(UiSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        var screenClass = snapshot.screenClass();
        if (screenClass == null || screenClass.isBlank()) {
            return false;
        }
        if (!screenClass.startsWith("net.minecraft.client.gui.screens.")) {
            return false;
        }
        if (PAUSE_SCREEN_CLASS.equals(screenClass)) {
            return false;
        }
        if (screenClass.startsWith("net.minecraft.client.gui.screens.worldselection.")) {
            return false;
        }
        return snapshot.targets().stream().noneMatch(VanillaUiCaptureAvailability::usesSelectionListRendering);
    }

    static boolean supportsFramebufferCapture(UiContext context) {
        try {
            var minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            var instance = minecraftClass.getMethod("getInstance").invoke(null);
            if (instance == null) {
                return false;
            }
            var screen = minecraftClass.getField("screen").get(instance);
            if (screen == null) {
                return "custom.UnknownScreen".equals(context.screenClass());
            }
            return screen.getClass().getName().equals(context.screenClass());
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean usesSelectionListRendering(UiTarget target) {
        var widgetClass = target.extensions().get("widgetClass");
        if (!(widgetClass instanceof String className) || className.isBlank()) {
            return false;
        }
        return className.contains("SelectionList");
    }
}
