package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.ModDevApi;

public final class BuiltinUiCaptureProviders {

    private BuiltinUiCaptureProviders() {
    }

    public static void register(ModDevApi api) {
        api.registerUiOffscreenCaptureProvider(new VanillaOffscreenCaptureProvider());
        api.registerUiFramebufferCaptureProvider(new VanillaFramebufferCaptureProvider());
    }
}

