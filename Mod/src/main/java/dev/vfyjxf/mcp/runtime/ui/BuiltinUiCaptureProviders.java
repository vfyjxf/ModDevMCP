package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.ModMcpApi;

public final class BuiltinUiCaptureProviders {

    private BuiltinUiCaptureProviders() {
    }

    public static void register(ModMcpApi api) {
        api.registerUiOffscreenCaptureProvider(new VanillaOffscreenCaptureProvider());
        api.registerUiFramebufferCaptureProvider(new VanillaFramebufferCaptureProvider());
    }
}
