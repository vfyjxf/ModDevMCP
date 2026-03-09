package dev.vfyjxf.mcp.server.api;

import java.util.Optional;

@FunctionalInterface
public interface McpResourceProvider {

    Optional<McpResource> read(String uri);
}
