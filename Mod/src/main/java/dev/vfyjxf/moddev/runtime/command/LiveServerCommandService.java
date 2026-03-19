package dev.vfyjxf.moddev.runtime.command;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class LiveServerCommandService implements CommandService {

    private static final long EXECUTION_TIMEOUT_SECONDS = 5L;

    @Override
    public CommandListResult list(CommandQuery query) {
        return onServerThread(server -> BrigadierCommandSupport.list(
                server.getCommands().getDispatcher(),
                server.createCommandSourceStack().withPermission(net.minecraft.commands.Commands.LEVEL_OWNERS),
                CommandType.SERVER,
                query
        ));
    }

    @Override
    public CommandSuggestionResult suggest(CommandSuggestionQuery query) {
        return onServerThread(server -> BrigadierCommandSupport.suggest(
                server.getCommands().getDispatcher(),
                server.createCommandSourceStack().withPermission(net.minecraft.commands.Commands.LEVEL_OWNERS),
                query
        ));
    }

    @Override
    public CommandExecutionResult execute(CommandExecutionRequest request) {
        return onServerThread(server -> BrigadierCommandSupport.execute(
                server.getCommands().getDispatcher(),
                server.createCommandSourceStack().withPermission(net.minecraft.commands.Commands.LEVEL_OWNERS),
                request
        ));
    }

    private <T> T onServerThread(java.util.function.Function<net.minecraft.server.MinecraftServer, T> action) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new CommandServiceException("command_runtime_unavailable", "Server command source is unavailable");
        }
        try {
            if (server.isSameThread()) {
                return action.apply(server);
            }
            var future = new CompletableFuture<T>();
            server.execute(() -> {
                try {
                    future.complete(action.apply(server));
                } catch (Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });
            return future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (CommandServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CommandServiceException("command_runtime_unavailable", exception.getMessage());
        }
    }
}

