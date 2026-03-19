package dev.vfyjxf.moddev.runtime.tool;

import dev.vfyjxf.moddev.runtime.command.CommandDescriptor;
import dev.vfyjxf.moddev.runtime.command.CommandExecutionRequest;
import dev.vfyjxf.moddev.runtime.command.CommandExecutionResult;
import dev.vfyjxf.moddev.runtime.command.CommandListResult;
import dev.vfyjxf.moddev.runtime.command.CommandQuery;
import dev.vfyjxf.moddev.runtime.command.CommandService;
import dev.vfyjxf.moddev.runtime.command.CommandSuggestion;
import dev.vfyjxf.moddev.runtime.command.CommandSuggestionQuery;
import dev.vfyjxf.moddev.runtime.command.CommandSuggestionResult;
import dev.vfyjxf.moddev.runtime.command.CommandType;
import dev.vfyjxf.moddev.server.ModDevMcpServer;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandToolProviderTest {

    @Test
    void commandProviderRegistersExpectedToolNames() {
        var registry = new McpToolRegistry();
        CommandToolProvider.serverOnly(new RecordingServerCommandService()).register(registry);

        assertTrue(registry.findTool("moddev.command_list", "server").isPresent());
        assertTrue(registry.findTool("moddev.command_suggest", "server").isPresent());
        assertTrue(registry.findTool("moddev.command_execute", "server").isPresent());
    }

    @Test
    void commandProviderDefinesSchemaForCommandTools() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        CommandToolProvider.serverOnly(new RecordingServerCommandService()).register(server.registry());

        var list = server.registry().findTool("moddev.command_list", "server").orElseThrow().definition();
        var suggest = server.registry().findTool("moddev.command_suggest", "server").orElseThrow().definition();
        var execute = server.registry().findTool("moddev.command_execute", "server").orElseThrow().definition();

        assertEquals("server", list.side());
        assertTrue(((Map<?, ?>) list.inputSchema().get("properties")).containsKey("query"));
        assertTrue(((Map<?, ?>) list.inputSchema().get("properties")).containsKey("targetSide"));
        assertTrue(!((Map<?, ?>) list.inputSchema().get("properties")).containsKey("runtimeSide"));
        assertTrue(!((Map<?, ?>) list.inputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(((Map<?, ?>) list.outputSchema().get("properties")).containsKey("commands"));
        assertTrue(!((Map<?, ?>) list.outputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(list.description().contains("selected runtime"));

        assertEquals("server", suggest.side());
        assertTrue(((Map<?, ?>) suggest.inputSchema().get("properties")).containsKey("input"));
        assertTrue(((Map<?, ?>) suggest.inputSchema().get("properties")).containsKey("targetSide"));
        assertTrue(!((Map<?, ?>) suggest.inputSchema().get("properties")).containsKey("runtimeSide"));
        assertTrue(!((Map<?, ?>) suggest.inputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(((Map<?, ?>) suggest.outputSchema().get("properties")).containsKey("suggestions"));
        assertTrue(!((Map<?, ?>) suggest.outputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(suggest.description().contains("selected runtime"));

        assertEquals("server", execute.side());
        assertTrue(((Map<?, ?>) execute.inputSchema().get("properties")).containsKey("command"));
        assertTrue(((Map<?, ?>) execute.inputSchema().get("properties")).containsKey("targetSide"));
        assertTrue(!((Map<?, ?>) execute.inputSchema().get("properties")).containsKey("runtimeSide"));
        assertTrue(!((Map<?, ?>) execute.inputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(((Map<?, ?>) execute.outputSchema().get("properties")).containsKey("messages"));
        assertTrue(!((Map<?, ?>) execute.outputSchema().get("properties")).containsKey("commandSide"));
        assertTrue(execute.description().contains("selected runtime"));
    }

    @Test
    void commandExecuteDelegatesToServiceAndReturnsStructuredPayload() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var serverService = new RecordingServerCommandService();
        CommandToolProvider.serverOnly(serverService).register(server.registry());

        var result = server.registry().findTool("moddev.command_execute", "server").orElseThrow()
                .handler()
                .handle(dev.vfyjxf.moddev.server.api.ToolCallContext.empty(), Map.of("command", "/say hi"));

        assertTrue(result.success());
        assertEquals("say hi", serverService.lastExecution.command());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals(true, payload.get("executed"));
        assertEquals(List.of("ok"), payload.get("messages"));
    }

    @Test
    void clientOnlyProviderUsesClientService() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var clientService = new RecordingClientCommandService();
        CommandToolProvider.clientOnly(clientService).register(server.registry());

        var result = server.registry().findTool("moddev.command_list", "client").orElseThrow()
                .handler()
                .handle(new dev.vfyjxf.moddev.server.api.ToolCallContext("client", Map.of("runtimeId", "client-runtime")), Map.of());

        assertTrue(result.success());
        assertEquals(1, clientService.listCalls);
    }

    @Test
    void serverOnlyProviderUsesServerService() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var serverService = new RecordingServerCommandService();
        CommandToolProvider.serverOnly(serverService).register(server.registry());

        var result = server.registry().findTool("moddev.command_list", "server").orElseThrow()
                .handler()
                .handle(new dev.vfyjxf.moddev.server.api.ToolCallContext("server", Map.of("runtimeId", "server-runtime")), Map.of());

        assertTrue(result.success());
        assertEquals(1, serverService.listCalls);
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

