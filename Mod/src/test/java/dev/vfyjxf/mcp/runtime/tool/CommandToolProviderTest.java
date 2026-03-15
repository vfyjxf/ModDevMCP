package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.command.CommandDescriptor;
import dev.vfyjxf.mcp.runtime.command.CommandExecutionResult;
import dev.vfyjxf.mcp.runtime.command.CommandService;
import dev.vfyjxf.mcp.runtime.command.CommandSuggestion;
import dev.vfyjxf.mcp.runtime.command.CommandType;
import dev.vfyjxf.mcp.runtime.command.CommandQuery;
import dev.vfyjxf.mcp.runtime.command.CommandSuggestionQuery;
import dev.vfyjxf.mcp.runtime.command.CommandExecutionRequest;
import dev.vfyjxf.mcp.runtime.command.CommandListResult;
import dev.vfyjxf.mcp.runtime.command.CommandSuggestionResult;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandToolProviderTest {

    @Test
    void commandProviderRegistersExpectedToolNames() {
        var registry = new McpToolRegistry();
        CommandToolProvider.clientAndServer(new RecordingClientCommandService(), new RecordingServerCommandService()).register(registry);

        assertTrue(registry.findTool("moddev.command_list").isPresent());
        assertTrue(registry.findTool("moddev.command_suggest").isPresent());
        assertTrue(registry.findTool("moddev.command_execute").isPresent());
    }

    @Test
    void commandProviderDefinesSchemaForCommandTools() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        CommandToolProvider.clientAndServer(new RecordingClientCommandService(), new RecordingServerCommandService()).register(server.registry());

        var list = server.registry().findTool("moddev.command_list").orElseThrow().definition();
        var suggest = server.registry().findTool("moddev.command_suggest").orElseThrow().definition();
        var execute = server.registry().findTool("moddev.command_execute").orElseThrow().definition();

        assertEquals("common", list.side());
        assertTrue(((Map<?, ?>) list.inputSchema().get("properties")).containsKey("query"));
        assertTrue(((Map<?, ?>) list.inputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(!((Map<?, ?>) list.inputSchema().get("properties")).containsKey("runtimeSide"));
        assertTrue(((Map<?, ?>) list.outputSchema().get("properties")).containsKey("commands"));
        assertTrue(((Map<?, ?>) list.outputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(list.description().contains("command context"));
        assertTrue(((Map<?, ?>) ((Map<?, ?>) list.inputSchema().get("properties")).get("commandSide")).get("description").toString().contains("command"));

        assertEquals("common", suggest.side());
        assertTrue(((Map<?, ?>) suggest.inputSchema().get("properties")).containsKey("input"));
        assertTrue(((Map<?, ?>) suggest.inputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(!((Map<?, ?>) suggest.inputSchema().get("properties")).containsKey("runtimeSide"));
        assertTrue(((Map<?, ?>) suggest.outputSchema().get("properties")).containsKey("suggestions"));
        assertTrue(((Map<?, ?>) suggest.outputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(suggest.description().contains("dispatcher"));
        assertTrue(((Map<?, ?>) ((Map<?, ?>) suggest.inputSchema().get("properties")).get("commandSide")).get("description").toString().contains("inside"));

        assertEquals("common", execute.side());
        assertTrue(((Map<?, ?>) execute.inputSchema().get("properties")).containsKey("command"));
        assertTrue(((Map<?, ?>) execute.inputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(!((Map<?, ?>) execute.inputSchema().get("properties")).containsKey("runtimeSide"));
        assertTrue(((Map<?, ?>) execute.outputSchema().get("properties")).containsKey("messages"));
        assertTrue(((Map<?, ?>) execute.outputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(execute.description().contains("available runtime"));
        assertTrue(execute.description().contains("command context"));
    }

    @Test
    void commandExecuteDelegatesToServiceAndReturnsStructuredPayload() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var clientService = new RecordingClientCommandService();
        var serverService = new RecordingServerCommandService();
        CommandToolProvider.clientAndServer(clientService, serverService).register(server.registry());

        var result = server.registry().findTool("moddev.command_execute").orElseThrow()
                .handler()
                .handle(dev.vfyjxf.mcp.server.api.ToolCallContext.empty(), Map.of("command", "/say hi"));

        assertTrue(result.success());
        assertEquals("say hi", serverService.lastExecution.command());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(true, payload.get("executed"));
        assertEquals("server", payload.get("commandSide"));
        assertEquals(List.of("ok"), payload.get("messages"));
    }

    @Test
    void commandProviderRoutesClientRequestsToClientService() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var clientService = new RecordingClientCommandService();
        var serverService = new RecordingServerCommandService();
        CommandToolProvider.clientAndServer(clientService, serverService).register(server.registry());

        var result = server.registry().findTool("moddev.command_list").orElseThrow()
                .handler()
                .handle(new dev.vfyjxf.mcp.server.api.ToolCallContext("client", Map.of("runtimeId", "client-runtime")), Map.of("commandSide", "client"));

        assertTrue(result.success());
        assertEquals(1, clientService.listCalls);
        assertEquals(0, serverService.listCalls);
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("client", payload.get("commandSide"));
    }

    @Test
    void commandProviderCanRouteServerRequestsInsideClientRuntime() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var clientService = new RecordingClientCommandService();
        var serverService = new RecordingServerCommandService();
        CommandToolProvider.clientAndServer(clientService, serverService).register(server.registry());

        var result = server.registry().findTool("moddev.command_list").orElseThrow()
                .handler()
                .handle(new dev.vfyjxf.mcp.server.api.ToolCallContext("client", Map.of("runtimeId", "client-runtime")), Map.of("commandSide", "server"));

        assertTrue(result.success());
        assertEquals(0, clientService.listCalls);
        assertEquals(1, serverService.listCalls);
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("server", payload.get("commandSide"));
    }

    private static final class RecordingClientCommandService implements CommandService {
        private int listCalls;

        @Override
        public CommandListResult list(CommandQuery query) {
            listCalls++;
            return new CommandListResult(List.of(
                    new CommandDescriptor("clientconfig", "/clientconfig", "mod", "neoforge", "Open client config", CommandType.CLIENT)
            ), 1, false);
        }

        @Override
        public CommandSuggestionResult suggest(CommandSuggestionQuery query) {
            return new CommandSuggestionResult("cli", 3, List.of(
                    new CommandSuggestion("clientconfig", 0, 3, "Open client config")
            ));
        }

        @Override
        public CommandExecutionResult execute(CommandExecutionRequest request) {
            return CommandExecutionResult.success(request.command(), 1, List.of("client ok"));
        }
    }

    private static final class RecordingServerCommandService implements CommandService {
        private CommandExecutionRequest lastExecution;
        private int listCalls;

        @Override
        public CommandListResult list(CommandQuery query) {
            listCalls++;
            return new CommandListResult(List.of(
                    new CommandDescriptor("say", "/say <message>", "minecraft", "minecraft", "Broadcast a message", CommandType.SERVER)
            ), 1, false);
        }

        @Override
        public CommandSuggestionResult suggest(CommandSuggestionQuery query) {
            return new CommandSuggestionResult("sa", 2, List.of(
                    new CommandSuggestion("say", 0, 2, "Broadcast a message")
            ));
        }

        @Override
        public CommandExecutionResult execute(CommandExecutionRequest request) {
            this.lastExecution = request;
            return CommandExecutionResult.success(request.command(), 1, List.of("ok"));
        }
    }
}
