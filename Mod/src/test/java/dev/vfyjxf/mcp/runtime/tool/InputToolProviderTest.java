package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.ClientScreenMetrics;
import dev.vfyjxf.mcp.api.runtime.InputController;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InputToolProviderTest {

    @Test
    void inputActionDelegatesToFirstAcceptingController() {
        var registries = new RuntimeRegistries();
        var first = new RecordingInputController(OperationResult.rejected("unsupported"));
        var second = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(first);
        registries.inputControllers().add(second);

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "click",
                "x", 427,
                "y", 169
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("click", payload.get("action"));
        assertEquals(true, payload.get("performed"));
        assertEquals(1, first.calls.get());
        assertEquals(1, second.calls.get());
        assertEquals(Map.of("action", "click", "x", 427, "y", 169), second.lastArguments);
    }

    @Test
    void inputActionReturnsFailureWhenNoControllerAcceptsAction() {
        var registries = new RuntimeRegistries();
        registries.inputControllers().add(new RecordingInputController(OperationResult.rejected("unsupported")));

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "click",
                "x", 10,
                "y", 20
        ));

        assertFalse(result.success());
        assertEquals("unsupported: no input controller accepted action click (dev.vfyjxf.mcp.runtime.tool.InputToolProviderTest$RecordingInputController: unsupported)", result.error());
    }

    @Test
    void inputActionForwardsHoverArgumentsToController() {
        var registries = new RuntimeRegistries();
        var controller = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(controller);

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "hover",
                "x", 200,
                "y", 100,
                "hoverDelayMs", 120
        ));

        assertTrue(result.success());
        assertEquals(Map.of(
                "action", "hover",
                "x", 200,
                "y", 100,
                "hoverDelayMs", 120
        ), controller.lastArguments);
    }

    @Test
    void inputActionForwardsUiIntentArgumentsToController() {
        var registries = new RuntimeRegistries();
        var controller = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(controller);

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "ui_intent",
                "intent", "pause_menu"
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("ui_intent", payload.get("action"));
        assertEquals(Map.of(
                "action", "ui_intent",
                "intent", "pause_menu"
        ), controller.lastArguments);
    }

    @Test
    void inputActionContinuesPastUnsupportedIntentController() {
        var registries = new RuntimeRegistries();
        var first = new RecordingInputController(OperationResult.rejected("unsupported_intent"));
        var second = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(first);
        registries.inputControllers().add(second);

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "ui_intent",
                "intent", "inventory"
        ));

        assertTrue(result.success());
        assertEquals(1, first.calls.get());
        assertEquals(1, second.calls.get());
    }

    @Test
    void inputActionForwardsRawKeyDownArgumentsToController() {
        var registries = new RuntimeRegistries();
        var controller = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(controller);

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "key_down",
                "keyCode", 341
        ));

        assertTrue(result.success());
        assertEquals(Map.of(
                "action", "key_down",
                "keyCode", 341
        ), controller.lastArguments);
    }

    @Test
    void inputActionForwardsRawKeyUpArgumentsToController() {
        var registries = new RuntimeRegistries();
        var controller = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(controller);

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "key_up",
                "keyCode", 341
        ));

        assertTrue(result.success());
        assertEquals(Map.of(
                "action", "key_up",
                "keyCode", 341
        ), controller.lastArguments);
    }

    @Test
    void inputActionForwardsRawMouseDownArgumentsToController() {
        var registries = new RuntimeRegistries();
        var controller = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(controller);

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "mouse_down",
                "x", 48,
                "y", 72,
                "button", 0
        ));

        assertTrue(result.success());
        assertEquals(Map.of(
                "action", "mouse_down",
                "x", 48,
                "y", 72,
                "button", 0
        ), controller.lastArguments);
    }

    @Test
    void inputActionForwardsRawMouseUpArgumentsToController() {
        var registries = new RuntimeRegistries();
        var controller = new RecordingInputController(OperationResult.success(null));
        registries.inputControllers().add(controller);

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "mouse_up",
                "x", 48,
                "y", 72,
                "button", 0
        ));

        assertTrue(result.success());
        assertEquals(Map.of(
                "action", "mouse_up",
                "x", 48,
                "y", 72,
                "button", 0
        ), controller.lastArguments);
    }

    @Test
    void inputActionIncludesInWorldStateWhenNoScreenIsOpen() {
        var registries = new RuntimeRegistries();
        registries.inputControllers().add(new RecordingInputController(OperationResult.success(null)));

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(registries, () -> new ClientScreenMetrics(null, 0, 0, 0, 0)).register(server.registry());

        var tool = server.registry().findTool("moddev.input_action").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "action", "key_down",
                "keyCode", 341
        ));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("", payload.get("screenClass"));
        assertEquals(false, payload.get("screenAvailable"));
        assertEquals(true, payload.get("inWorld"));
    }

    @Test
    void clipboardSetUsesDedicatedClipboardTool() {
        var registries = new RuntimeRegistries();
        var clipboard = new AtomicReference<>("");

        var server = new ModDevMcpServer(new McpToolRegistry());
        new InputToolProvider(
                registries,
                () -> new ClientScreenMetrics(null, 0, 0, 0, 0),
                clipboard::set
        ).register(server.registry());

        var tool = server.registry().findTool("moddev.input_clipboard_set").orElseThrow();
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "text", "paste-me"
        ));

        assertTrue(result.success());
        assertEquals("paste-me", clipboard.get());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(true, payload.get("performed"));
        assertEquals(8, payload.get("textLength"));
        assertEquals(true, payload.get("inWorld"));
    }

    private static final class RecordingInputController implements InputController {

        private final OperationResult<Void> result;
        private final AtomicInteger calls = new AtomicInteger();
        private Map<String, Object> lastArguments = Map.of();

        private RecordingInputController(OperationResult<Void> result) {
            this.result = result;
        }

        @Override
        public OperationResult<Void> perform(String action, Map<String, Object> arguments) {
            calls.incrementAndGet();
            lastArguments = Map.copyOf(arguments);
            return result;
        }
    }
}
