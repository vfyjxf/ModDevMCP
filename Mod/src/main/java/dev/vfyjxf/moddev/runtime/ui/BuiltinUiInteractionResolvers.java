package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.ModMcpApi;
import dev.vfyjxf.moddev.api.runtime.UiInteractionStateResolver;
import dev.vfyjxf.moddev.api.ui.UiInteractionDefaults;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import dev.vfyjxf.moddev.runtime.UiInteractionStateResolverRegistry;

import java.util.List;

public final class BuiltinUiInteractionResolvers {

    private BuiltinUiInteractionResolvers() {
    }

    public static UiInteractionStateResolverRegistry newRegistry() {
        var registry = new UiInteractionStateResolverRegistry();
        registerInto(registry::register);
        return registry;
    }

    public static void register(ModMcpApi api) {
        registerInto(api::registerUiInteractionStateResolver);
    }

    private static void registerInto(UiInteractionResolverSink sink) {
        sink.register(new FixedUiInteractionStateResolver(
            "vanilla-container",
            200,
            "container-root",
            "container-root",
            "container-root",
            "container-root",
            "builtin"
        ));
        sink.register(new FixedUiInteractionStateResolver(
            "vanilla-screen",
            100,
            null,
            null,
            null,
            null,
            "builtin"
        ));
        sink.register(new FixedUiInteractionStateResolver(
            "fallback-region",
            0,
            null,
            null,
            null,
            null,
            "builtin"
        ));
    }

    private record FixedUiInteractionStateResolver(
            String driverId,
            int priority,
            String focusedTargetId,
            String selectedTargetId,
            String hoveredTargetId,
            String activeTargetId,
            String selectionSource
    ) implements UiInteractionStateResolver {

        @Override
        public boolean matches(dev.vfyjxf.moddev.api.runtime.UiContext context, List<UiTarget> targets) {
            return true;
        }

        @Override
        public UiInteractionDefaults resolve(dev.vfyjxf.moddev.api.runtime.UiContext context, List<UiTarget> targets) {
            return new UiInteractionDefaults(
                    focusedTargetId,
                    selectedTargetId,
                    hoveredTargetId,
                    activeTargetId,
                    selectionSource
            );
        }
    }

    @FunctionalInterface
    private interface UiInteractionResolverSink {
        void register(UiInteractionStateResolver resolver);
    }
}

