package dev.vfyjxf.mcp.runtime.input;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.ClientScreenMetrics;
import dev.vfyjxf.mcp.runtime.ui.UiPointerStateRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftInputControllerTest {

    @Test
    void clickActionConvertsFramebufferCoordinatesBeforeDispatch() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge);

        var result = controller.perform("click", Map.of(
                "x", 427,
                "y", 168,
                "button", 0,
                "coordinateSpace", "framebuffer"
        ));

        assertTrue(result.accepted());
        assertEquals("click", bridge.lastCommand.action());
        assertEquals(284.5d, bridge.lastCommand.x());
        assertEquals(112.0d, bridge.lastCommand.y());
        assertEquals(0, bridge.lastCommand.button());
    }

    @Test
    void moveActionUsesGuiCoordinatesByDefault() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge);

        var result = controller.perform("move", Map.of(
                "x", 120,
                "y", 64
        ));

        assertTrue(result.accepted());
        assertEquals("move", bridge.lastCommand.action());
        assertEquals(120.0d, bridge.lastCommand.x());
        assertEquals(64.0d, bridge.lastCommand.y());
    }

    @Test
    void hoverActionIncludesHoverDelayMs() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge);

        var result = controller.perform("hover", Map.of(
                "x", 180,
                "y", 90,
                "hoverDelayMs", 150
        ));

        assertTrue(result.accepted());
        assertEquals("hover", bridge.lastCommand.action());
        assertEquals(180.0d, bridge.lastCommand.x());
        assertEquals(90.0d, bridge.lastCommand.y());
        assertEquals(150, bridge.lastCommand.durationMs());
    }

    @Test
    void hoverActionTracksPointerPositionForSubsequentUiTools() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var pointerStates = new UiPointerStateRegistry();
        var controller = new MinecraftInputController(bridge, pointerStates);

        var result = controller.perform("hover", Map.of(
                "x", 180,
                "y", 90
        ));

        assertTrue(result.accepted());
        var pointer = pointerStates.stateFor("net.minecraft.client.gui.screens.TitleScreen", "minecraft");
        assertEquals(180, pointer.mouseX());
        assertEquals(90, pointer.mouseY());
    }

    @Test
    void clickActionRejectsWhenExpectedScreenDoesNotMatch() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge);

        var result = controller.perform("click", Map.of(
                "screenClass", "net.minecraft.client.gui.screens.worldselection.SelectWorldScreen",
                "x", 100,
                "y", 50
        ));

        assertFalse(result.accepted());
        assertEquals("screen_mismatch: expected net.minecraft.client.gui.screens.worldselection.SelectWorldScreen but was net.minecraft.client.gui.screens.TitleScreen", result.reason());
    }

    @Test
    void typeTextDispatchesLiteralTextCommand() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.worldselection.CreateWorldScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge);

        var result = controller.perform("type_text", Map.of(
                "text", "New World"
        ));

        assertTrue(result.accepted());
        assertEquals("type_text", bridge.lastCommand.action());
        assertEquals("New World", bridge.lastCommand.text());
    }

    @Test
    void uiIntentInventoryDispatchesKeyPressCommand() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.inventory.InventoryScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge, new UiPointerStateRegistry(), intent -> "inventory".equals(intent) ? 69 : -1);

        var result = controller.perform("ui_intent", Map.of(
                "intent", "inventory"
        ));

        assertTrue(result.accepted());
        assertEquals("key_press", bridge.lastCommand.action());
        assertEquals(69, bridge.lastCommand.keyCode());
    }

    @Test
    void uiIntentUsesInjectedKeyResolver() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.inventory.InventoryScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge, new UiPointerStateRegistry(), intent -> "inventory".equals(intent) ? 321 : -1);

        var result = controller.perform("ui_intent", Map.of(
                "intent", "inventory"
        ));

        assertTrue(result.accepted());
        assertEquals("key_press", bridge.lastCommand.action());
        assertEquals(321, bridge.lastCommand.keyCode());
    }

    private static final class RecordingClientInputBridge implements ClientInputBridge {

        private final ClientScreenMetrics metrics;
        private final OperationResult<Void> result;
        private InputCommand lastCommand;

        private RecordingClientInputBridge(ClientScreenMetrics metrics, OperationResult<Void> result) {
            this.metrics = metrics;
            this.result = result;
        }

        @Override
        public ClientScreenMetrics metrics() {
            return metrics;
        }

        @Override
        public OperationResult<Void> execute(InputCommand command) {
            this.lastCommand = command;
            return result;
        }
    }
}
