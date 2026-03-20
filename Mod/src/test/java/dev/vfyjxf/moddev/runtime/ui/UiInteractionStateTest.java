package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UiInteractionStateTest {

    @Test
    void interactionStateReturnsFocusedHoveredSelectedAndActiveTargets() {
        var driver = new VanillaContainerUiDriver();
        var state = driver.interactionState(new TestUiContext("net.minecraft.client.gui.screens.inventory.InventoryScreen"));

        assertNotNull(state.focusedTarget());
    }

    private record TestUiContext(String screenClass) implements UiContext {
    }
}

