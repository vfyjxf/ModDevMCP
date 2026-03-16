package dev.vfyjxf.mcp.api.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class UiContextTest {

    @Test
    void screenHandleDefaultsToNull() {
        UiContext context = new UiContext() {
            @Override
            public String screenClass() {
                return "example.Screen";
            }
        };

        assertNull(context.screenHandle());
    }
}
