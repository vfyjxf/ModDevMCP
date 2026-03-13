package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.ui.Bounds;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;
import dev.vfyjxf.mcp.api.ui.UiTargetState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaUiCaptureAvailabilityTest {

    @Test
    void offscreenCaptureSupportsSimpleVanillaButtonScreens() {
        var snapshot = new UiSnapshot(
                "screen",
                "net.minecraft.client.gui.screens.TitleScreen",
                "vanilla-screen",
                List.of(target(
                        "button-singleplayer",
                        "Singleplayer",
                        Map.of("widgetClass", "net.minecraft.client.gui.components.Button")
                )),
                List.of(),
                null,
                null,
                null,
                null,
                Map.of()
        );

        assertTrue(VanillaUiCaptureAvailability.supportsOffscreenSnapshot(snapshot));
    }

    @Test
    void offscreenCaptureRejectsSelectionListScreens() {
        var snapshot = new UiSnapshot(
                "screen",
                "net.minecraft.client.gui.screens.worldselection.SelectWorldScreen",
                "vanilla-screen",
                List.of(target(
                        "button-1",
                        "",
                        Map.of("widgetClass", "net.minecraft.client.gui.screens.worldselection.WorldSelectionList")
                )),
                List.of(),
                null,
                null,
                null,
                null,
                Map.of()
        );

        assertFalse(VanillaUiCaptureAvailability.supportsOffscreenSnapshot(snapshot));
    }

    @Test
    void offscreenCaptureRejectsWorldCreationScreens() {
        var snapshot = new UiSnapshot(
                "screen",
                "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen",
                "vanilla-screen",
                List.of(target(
                        "button-create-new-world",
                        "Create New World",
                        Map.of("widgetClass", "net.minecraft.client.gui.components.Button")
                )),
                List.of(),
                null,
                null,
                null,
                null,
                Map.of()
        );

        assertFalse(VanillaUiCaptureAvailability.supportsOffscreenSnapshot(snapshot));
    }

    private UiTarget target(String id, String text, Map<String, Object> extensions) {
        return new UiTarget(
                id,
                "vanilla-screen",
                "screen",
                "minecraft",
                "button",
                text,
                new Bounds(0, 0, 10, 10),
                UiTargetState.defaultState(),
                List.of("click"),
                extensions
        );
    }
}
