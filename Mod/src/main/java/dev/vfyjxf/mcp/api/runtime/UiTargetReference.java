package dev.vfyjxf.mcp.api.runtime;

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
