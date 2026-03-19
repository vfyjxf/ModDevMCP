package dev.vfyjxf.moddev.api.runtime;

/**
 * Input for resolving a target reference or locator into concrete UI targets.
 */
public record UiResolveRequest(
        UiTargetReference reference,
        boolean allowMultiple,
        boolean includeHidden,
        boolean includeDisabled
) {
}

