package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiContext;

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
}
