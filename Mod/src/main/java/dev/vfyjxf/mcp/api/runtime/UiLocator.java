package dev.vfyjxf.mcp.api.runtime;

public record UiLocator(
        String role,
        String text,
        String containsText,
        String id,
        Integer index,
        String scopeRef
) {
}
