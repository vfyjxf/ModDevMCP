package dev.vfyjxf.mcp.runtime.input;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.ClientScreenMetrics;
import dev.vfyjxf.mcp.runtime.ui.UiPointerStateRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftInputControllerTest {

    private static final int GLFW_KEY_LEFT_SHIFT = 340;
    private static final int GLFW_MOD_SHIFT = 0x0001;

    @Test
    void keyClickModifiersRemainRequestScopedAtControllerBoundary() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics(null, 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge, new UiPointerStateRegistry(), intent -> -1);

        controller.perform("key_click", Map.of("keyCode", 88, "modifiers", GLFW_MOD_SHIFT));
        controller.perform("key_down", Map.of("keyCode", 65));

        assertEquals(GLFW_MOD_SHIFT, bridge.recordedCommands().get(0).modifiers());
        assertEquals(0, bridge.recordedCommands().get(1).modifiers());
    }

    @Test
    void clientStartupClearsStaleVirtualModifierState() {
        VirtualModifierState.global().keyDown(GLFW_KEY_LEFT_SHIFT);

        VirtualModifierState.global().clear();

        assertEquals(0, VirtualModifierState.global().modifierBits());
    }

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
    void keyDownDispatchesLiteralRawKeyCommandWithoutScreen() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics(null, 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge);

        var result = controller.perform("key_down", Map.of(
                "keyCode", 341,
                "modifiers", 0
        ));

        assertTrue(result.accepted());
        assertEquals("key_down", bridge.lastCommand.action());
        assertEquals(341, bridge.lastCommand.keyCode());
    }

    @Test
    void keyUpDispatchesLiteralRawKeyCommandWithoutScreen() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics(null, 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge);

        var result = controller.perform("key_up", Map.of(
                "keyCode", 341,
                "modifiers", 0
        ));

        assertTrue(result.accepted());
        assertEquals("key_up", bridge.lastCommand.action());
        assertEquals(341, bridge.lastCommand.keyCode());
    }

    @Test
    void keyClickDispatchesLiteralRawKeyClickCommandWithoutScreen() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics(null, 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var controller = new MinecraftInputController(bridge);

        var result = controller.perform("key_click", Map.of(
                "keyCode", 88,
                "modifiers", 2
        ));

        assertTrue(result.accepted());
        assertEquals("key_click", bridge.lastCommand.action());
        assertEquals(88, bridge.lastCommand.keyCode());
        assertEquals(2, bridge.lastCommand.modifiers());
    }

    @Test
    void mouseDownTracksPointerPositionForSubsequentUiTools() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var pointerStates = new UiPointerStateRegistry();
        var controller = new MinecraftInputController(bridge, pointerStates);

        var result = controller.perform("mouse_down", Map.of(
                "x", 200,
                "y", 110,
                "button", 0
        ));

        assertTrue(result.accepted());
        var pointer = pointerStates.stateFor("net.minecraft.client.gui.screens.TitleScreen", "minecraft");
        assertEquals(200, pointer.mouseX());
        assertEquals(110, pointer.mouseY());
    }

    @Test
    void mouseUpTracksPointerPositionForSubsequentUiTools() {
        var bridge = new RecordingClientInputBridge(
                new ClientScreenMetrics("net.minecraft.client.gui.screens.TitleScreen", 569, 320, 854, 480),
                OperationResult.success(null)
        );
        var pointerStates = new UiPointerStateRegistry();
        var controller = new MinecraftInputController(bridge, pointerStates);

        var result = controller.perform("mouse_up", Map.of(
                "x", 220,
                "y", 90,
                "button", 0
        ));

        assertTrue(result.accepted());
        var pointer = pointerStates.stateFor("net.minecraft.client.gui.screens.TitleScreen", "minecraft");
        assertEquals(220, pointer.mouseX());
        assertEquals(90, pointer.mouseY());
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
        private final List<InputCommand> recordedCommands = new ArrayList<>();
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
            this.recordedCommands.add(command);
            return result;
        }

        private List<InputCommand> recordedCommands() {
            return List.copyOf(recordedCommands);
        }
    }
}
