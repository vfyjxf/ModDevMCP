package dev.vfyjxf.mcp.api.runtime;

/**
 * Canonical reference used to point at a target across inspect, act, and wait flows.
 */
public record UiTargetReference(
        String ref,
        UiLocator locator,
        Integer pointX,
        Integer pointY
) {

    public static UiTargetReference ref(String ref) {
        return new UiTargetReference(ref, null, null, null);
    }

    public static UiTargetReference locator(UiLocator locator) {
        return new UiTargetReference(null, locator, null, null);
    }

    public static UiTargetReference point(int x, int y) {
        return new UiTargetReference(null, null, x, y);
    }
}
