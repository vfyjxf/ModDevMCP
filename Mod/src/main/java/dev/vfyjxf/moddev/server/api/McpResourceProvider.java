package dev.vfyjxf.moddev.server.api;

import java.util.Optional;

@FunctionalInterface
public interface McpResourceProvider {

    Optional<McpResource> read(String uri);

    default java.util.List<McpResource> list() {
        return java.util.List.of();
    }
}

