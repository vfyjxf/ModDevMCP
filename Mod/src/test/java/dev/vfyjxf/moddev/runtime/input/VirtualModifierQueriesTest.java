package dev.vfyjxf.moddev.runtime.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualModifierQueriesTest {

    @Test
    void screenQueryHelperReturnsTrueWhenVirtualShiftIsActive() {
        assertTrue(VirtualModifierQueries.merge(false, true));
    }

    @Test
    void controlQueryIncludesVirtualSuperOnMacStylePaths() {
        assertTrue(VirtualModifierQueries.controlActive(false, false, true, true));
    }
}

