package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.ModMcpApi;

public final class BuiltinUiCaptureProviders {

    private BuiltinUiCaptureProviders() {
    }

    public static void register(ModMcpApi api) {
        api.registerUiOffscreenCaptureProvider(new VanillaOffscreenCaptureProvider());
        api.registerUiFramebufferCaptureProvider(new VanillaFramebufferCaptureProvider());
    }
}

