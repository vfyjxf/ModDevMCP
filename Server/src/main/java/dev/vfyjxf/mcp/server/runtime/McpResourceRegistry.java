package dev.vfyjxf.mcp.server.runtime;

import dev.vfyjxf.mcp.server.api.McpResource;
import dev.vfyjxf.mcp.server.api.McpResourceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class McpResourceRegistry {

    private final List<McpResourceProvider> providers = new ArrayList<>();

    public void registerProvider(McpResourceProvider provider) {
        providers.add(provider);
    }

    public Optional<McpResource> read(String uri) {
        return providers.stream()
                .map(provider -> provider.read(uri))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public List<McpResource> list() {
        return providers.stream()
                .flatMap(provider -> provider.list().stream())
                .toList();
    }
}
