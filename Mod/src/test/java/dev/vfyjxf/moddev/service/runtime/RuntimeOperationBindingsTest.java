package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.runtime.command.CommandExecutionRequest;
import dev.vfyjxf.moddev.runtime.command.CommandExecutionResult;
import dev.vfyjxf.moddev.runtime.command.CommandListResult;
import dev.vfyjxf.moddev.runtime.command.CommandQuery;
import dev.vfyjxf.moddev.runtime.command.CommandService;
import dev.vfyjxf.moddev.runtime.command.CommandSuggestionQuery;
import dev.vfyjxf.moddev.runtime.command.CommandSuggestionResult;
import dev.vfyjxf.moddev.service.request.OperationError;
import dev.vfyjxf.moddev.service.request.OperationExecutionException;
import dev.vfyjxf.moddev.service.request.OperationRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeOperationBindingsTest {

    @Test
    void operationRegistryCoversExistingCapabilityAreas() {
        var bindings = new RuntimeOperationBindings(new RuntimeRegistries(), statusProvider());

        var operationIds = bindings.operationRegistry().all().stream()
                .map(definition -> definition.operationId())
                .toList();

        assertTrue(operationIds.contains("status.get"));
        assertTrue(operationIds.contains("ui.inspect"));
        assertTrue(operationIds.contains("command.execute"));
        assertTrue(operationIds.contains("world.list"));
        assertTrue(operationIds.contains("hotswap.reload"));
    }

    @Test
    void statusOperationAllowsNullLastError() throws Exception {
        var bindings = new RuntimeOperationBindings(new RuntimeRegistries(), statusProvider());

        var output = bindings.execute(
                new OperationRequest("req-status", "status.get", null, Map.of()),
                null
        );

        assertEquals(true, output.get("serviceReady"));
        assertEquals(null, output.get("lastError"));
    }

    @Test
    void sideAwareOperationsUseRegisteredRuntimeService() throws Exception {
        var registries = new RuntimeRegistries();
        var commandService = new RecordingCommandService();
        commandService.executionResult = CommandExecutionResult.success("say hi", 1, List.of("ok"));
        registries.registerCommandService("server", commandService);

        var bindings = new RuntimeOperationBindings(registries, statusProvider());

        var output = bindings.execute(
                new OperationRequest("req-1", "command.execute", "server", Map.of("command", "/say hi")),
                "server"
        );

        assertEquals("say hi", commandService.executions.getFirst().command());
        assertEquals("server", output.get("runtimeSide"));
        assertEquals(true, output.get("executed"));
    }

    @Test
    void gameCloseRejectionReturnsStructuredExecutionErrors() {
        var registries = new RuntimeRegistries();
        registries.registerGameCloser("client", () -> false);
        var bindings = new RuntimeOperationBindings(registries, statusProvider());

        try {
            bindings.execute(
                    new OperationRequest("req-close", "status.game_close", "client", Map.of()),
                    "client"
            );
        } catch (OperationExecutionException exception) {
            OperationError error = exception.error();
            assertEquals("operation_execution_failed", error.errorCode());
            assertEquals("game_close_rejected", error.errorMessage());
            return;
        } catch (Exception exception) {
            throw new AssertionError("Expected OperationExecutionException", exception);
        }
        throw new AssertionError("Expected structured execution error");
    }

    private static RuntimeOperationBindings.StatusSnapshotProvider statusProvider() {
        return () -> new RuntimeOperationBindings.StatusSnapshot(
                true,
                true,
                List.of("client", "server"),
                "moddev-usage",
                Path.of("build/export"),
                null
        );
    }

    private static final class RecordingCommandService implements CommandService {
        private final List<CommandExecutionRequest> executions = new ArrayList<>();
        private CommandExecutionResult executionResult = CommandExecutionResult.success("", 0, List.of());

        @Override
        public CommandListResult list(CommandQuery query) {
            return new CommandListResult(List.of(), 0, false);
        }

        @Override
        public CommandSuggestionResult suggest(CommandSuggestionQuery query) {
            return new CommandSuggestionResult(query.input(), query.cursor(), List.of());
        }

        @Override
        public CommandExecutionResult execute(CommandExecutionRequest request) {
            executions.add(request);
            return executionResult;
        }
    }
}
