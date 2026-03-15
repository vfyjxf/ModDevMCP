package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinProviderRegistrationTest {

    @Test
    void modStartupRegistersBuiltinProvidersIntoServer() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        mod.registerBuiltinProviders();

        assertTrue(server.registry().findTool("moddev.ui_snapshot").isPresent());
        assertTrue(server.registry().findTool("moddev.ui_get_live_screen").isPresent());
        assertTrue(server.registry().findTool("moddev.inventory_snapshot").isPresent());
        assertTrue(server.registry().findTool("moddev.game_close").isPresent());
        assertTrue(server.registry().findTool("moddev.command_list").isPresent());
        assertTrue(server.registry().findTool("moddev.command_suggest").isPresent());
        assertTrue(server.registry().findTool("moddev.command_execute").isPresent());
        assertTrue(server.registry().findTool("moddev.compile").isPresent());
        assertTrue(server.registry().findTool("moddev.hotswap").isPresent());
    }

    @Test
    void modStartupKeepsClientOnlyRuntimeOutOfCommonRuntime() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        assertEquals(0, mod.registries().uiOffscreenCaptureProviders().size());
        assertEquals(0, mod.registries().uiFramebufferCaptureProviders().size());
        assertEquals(0, mod.registries().uiDrivers().size());
        assertEquals(0, mod.registries().inventoryDrivers().size());
        assertEquals(0, mod.registries().inputControllers().size());
    }

    @Test
    void clientServerPreparationRegistersBuiltinClientRuntime() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        mod.prepareClientServer();

        assertEquals(1, mod.registries().uiOffscreenCaptureProviders().size());
        assertEquals(1, mod.registries().uiFramebufferCaptureProviders().size());
        assertEquals(3, mod.registries().uiDrivers().size());
        assertEquals(1, mod.registries().inventoryDrivers().size());
        assertEquals(1, mod.registries().inputControllers().size());
    }

    @Test
    void modStartupRegistersCustomToolProvidersIntoServerWithoutDuplicates() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);
        mod.api().registerToolProvider(registry -> registry.registerTool(
                new McpToolDefinition("demo.extra", "Extra", "Extra tool", Map.of(), Map.of(), List.of(), "either", false, false, "public", "public"),
                (context, arguments) -> ToolResult.success(arguments)
        ));

        mod.registerBuiltinProviders();
        mod.registerBuiltinProviders();

        assertTrue(server.registry().findTool("demo.extra").isPresent());
        assertEquals(1, server.registry().listTools().stream()
                .filter(tool -> tool.definition().name().equals("demo.extra"))
                .count());
    }

    @Test
    void commonServerPreparationDoesNotRegisterClientOnlyTools() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        mod.prepareCommonServer();

        assertTrue(server.registry().findTool("moddev.event_subscribe").isPresent());
        assertTrue(server.registry().findTool("moddev.hotswap").isPresent());
        assertTrue(server.registry().findTool("moddev.ui_snapshot").isEmpty());
        assertTrue(server.registry().findTool("moddev.ui_get_live_screen").isEmpty());
        assertTrue(server.registry().findTool("moddev.input_action").isEmpty());
        assertTrue(server.registry().findTool("moddev.inventory_snapshot").isEmpty());
        assertTrue(server.registry().findTool("moddev.game_close").isEmpty());
    }

    @Test
    void clientServerPreparationRegistersClientOnlyTools() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        mod.prepareClientServer();

        assertTrue(server.registry().findTool("moddev.ui_snapshot").isPresent());
        assertTrue(server.registry().findTool("moddev.ui_get_live_screen").isPresent());
        assertTrue(server.registry().findTool("moddev.input_action").isPresent());
        assertTrue(server.registry().findTool("moddev.inventory_snapshot").isPresent());
        assertTrue(server.registry().findTool("moddev.game_close").isPresent());
        assertTrue(server.registry().findTool("moddev.command_list").isPresent());
        assertTrue(server.registry().findTool("moddev.command_suggest").isPresent());
        assertTrue(server.registry().findTool("moddev.command_execute").isPresent());
    }

    @Test
    void serverPreparationRegistersGameCloseWithoutClientOnlyTools() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(server);

        mod.prepareServer();

        assertTrue(server.registry().findTool("moddev.game_close").isPresent());
        assertTrue(server.registry().findTool("moddev.command_list").isPresent());
        assertTrue(server.registry().findTool("moddev.command_suggest").isPresent());
        assertTrue(server.registry().findTool("moddev.command_execute").isPresent());
        assertTrue(server.registry().findTool("moddev.ui_snapshot").isEmpty());
        assertTrue(server.registry().findTool("moddev.ui_get_live_screen").isEmpty());
        assertTrue(server.registry().findTool("moddev.input_action").isEmpty());
        assertTrue(server.registry().findTool("moddev.inventory_snapshot").isEmpty());
    }

    @Test
    void commonModClassDoesNotStoreClientOnlyProviderInstances() {
        var fieldNames = Set.of("uiToolProvider", "inputToolProvider", "inventoryToolProvider", "gameToolProvider");

        assertFalse(java.util.Arrays.stream(ModDevMCP.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .anyMatch(fieldNames::contains));
    }
}
