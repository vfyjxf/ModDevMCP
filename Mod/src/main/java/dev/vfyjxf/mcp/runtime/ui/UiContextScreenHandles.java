package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.UiContext;

public final class UiContextScreenHandles {

    private UiContextScreenHandles() {
    }

    public static Object raw(UiContext context) {
        return context == null ? null : context.screenHandle();
    }

    public static <T> T as(UiContext context, Class<T> type) {
        Object handle = raw(context);
        return type.isInstance(handle) ? type.cast(handle) : null;
    }
}
