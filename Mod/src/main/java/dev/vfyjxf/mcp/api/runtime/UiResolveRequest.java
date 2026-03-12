package dev.vfyjxf.mcp.api.runtime;

public record UiResolveRequest(
        UiTargetReference reference,
        boolean allowMultiple,
        boolean includeHidden,
        boolean includeDisabled
) {
}
