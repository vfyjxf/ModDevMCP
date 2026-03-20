package dev.vfyjxf.moddev.runtime;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiOffscreenCaptureProvider;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class UiOffscreenCaptureProviderRegistry {

    private final List<UiOffscreenCaptureProvider> providers = new ArrayList<>();

    public void register(UiOffscreenCaptureProvider provider) {
        providers.add(provider);
        providers.sort(Comparator.comparingInt(UiOffscreenCaptureProvider::priority).reversed());
    }

    public Optional<UiOffscreenCaptureProvider> select(UiContext context, UiSnapshot snapshot) {
        return providers.stream()
                .filter(provider -> provider.matches(context, snapshot))
                .findFirst();
    }

    public int size() {
        return providers.size();
    }
}

