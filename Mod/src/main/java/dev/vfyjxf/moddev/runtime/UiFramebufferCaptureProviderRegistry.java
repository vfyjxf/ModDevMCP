package dev.vfyjxf.moddev.runtime;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiFramebufferCaptureProvider;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class UiFramebufferCaptureProviderRegistry {

    private final List<UiFramebufferCaptureProvider> providers = new ArrayList<>();

    public void register(UiFramebufferCaptureProvider provider) {
        providers.add(provider);
        providers.sort(Comparator.comparingInt(UiFramebufferCaptureProvider::priority).reversed());
    }

    public Optional<UiFramebufferCaptureProvider> select(UiContext context, UiSnapshot snapshot) {
        return providers.stream()
                .filter(provider -> provider.matches(context, snapshot))
                .findFirst();
    }

    public int size() {
        return providers.size();
    }
}

