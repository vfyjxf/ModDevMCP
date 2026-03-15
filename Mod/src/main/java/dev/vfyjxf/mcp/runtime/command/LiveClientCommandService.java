package dev.vfyjxf.mcp.runtime.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class LiveClientCommandService implements CommandService {

    private static final long EXECUTION_TIMEOUT_SECONDS = 5L;

    @Override
    public CommandListResult list(CommandQuery query) {
        return onClientThread(() -> BrigadierCommandSupport.list(
                dispatcher(),
                requireSource(),
                CommandType.CLIENT,
                query
        ));
    }

    @Override
    public CommandSuggestionResult suggest(CommandSuggestionQuery query) {
        return onClientThread(() -> BrigadierCommandSupport.suggest(
                dispatcher(),
                requireSource(),
                query
        ));
    }

    @Override
    public CommandExecutionResult execute(CommandExecutionRequest request) {
        return onClientThread(() -> BrigadierCommandSupport.execute(
                dispatcher(),
                requireSource(),
                request
        ));
    }

    @SuppressWarnings("unchecked")
    private CommandDispatcher<CommandSourceStack> dispatcher() {
        try {
            var handlerClass = Class.forName("net.neoforged.neoforge.client.ClientCommandHandler");
            return (CommandDispatcher<CommandSourceStack>) handlerClass.getMethod("getDispatcher").invoke(null);
        } catch (Exception exception) {
            throw new CommandServiceException("command_runtime_unavailable", exception.getMessage());
        }
    }

    private CommandSourceStack requireSource() {
        try {
            var handlerClass = Class.forName("net.neoforged.neoforge.client.ClientCommandHandler");
            var source = handlerClass.getMethod("getSource").invoke(null);
            if (source instanceof CommandSourceStack commandSourceStack) {
                return commandSourceStack;
            }
            throw new CommandServiceException("command_runtime_unavailable", "Client command source is unavailable");
        } catch (CommandServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CommandServiceException("command_runtime_unavailable", exception.getMessage());
        }
    }

    private <T> T onClientThread(java.util.function.Supplier<T> action) {
        try {
            var minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                throw new CommandServiceException("command_runtime_unavailable", "Minecraft client is unavailable");
            }
            if (minecraft.isSameThread()) {
                return action.get();
            }
            var future = new CompletableFuture<T>();
            minecraft.execute(() -> {
                try {
                    future.complete(action.get());
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
