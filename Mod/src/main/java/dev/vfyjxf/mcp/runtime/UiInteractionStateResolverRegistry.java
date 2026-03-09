package dev.vfyjxf.mcp.runtime;

import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiInteractionStateResolver;
import dev.vfyjxf.mcp.api.ui.UiInteractionDefaults;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class UiInteractionStateResolverRegistry {

    private final List<UiInteractionStateResolver> resolvers = new ArrayList<>();

    public void register(UiInteractionStateResolver resolver) {
        resolvers.add(resolver);
        resolvers.sort(Comparator.comparingInt(UiInteractionStateResolver::priority).reversed());
    }

    public UiInteractionDefaults resolve(String driverId, UiContext context, List<UiTarget> targets) {
        return resolvers.stream()
                .filter(resolver -> resolver.driverId().equals(driverId))
                .filter(resolver -> resolver.matches(context, targets))
                .findFirst()
                .map(resolver -> resolver.resolve(context, targets))
                .orElse(UiInteractionDefaults.empty());
    }
}
