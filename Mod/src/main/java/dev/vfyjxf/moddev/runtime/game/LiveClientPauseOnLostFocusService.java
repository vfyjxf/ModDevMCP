package dev.vfyjxf.moddev.runtime.game;

import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class LiveClientPauseOnLostFocusService implements PauseOnLostFocusService {

    private static final long EXECUTION_TIMEOUT_SECONDS = 5L;

    @Override
    public boolean currentState() {
        return onClientThread(() -> requireMinecraft().options.pauseOnLostFocus);
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        return onClientThread(() -> {
            var minecraft = requireMinecraft();
            var options = minecraft.options;
            boolean changed = options.pauseOnLostFocus != enabled;
            options.pauseOnLostFocus = enabled;
            options.save();
            return changed;
        });
    }

    private Minecraft requireMinecraft() {
        var minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            throw new IllegalStateException("Minecraft client options are unavailable");
        }
        return minecraft;
    }

    private <T> T onClientThread(Supplier<T> action) {
        try {
            var minecraft = requireMinecraft();
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
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(Objects.toString(exception.getMessage(), exception.getClass().getSimpleName()), exception);
        }
    }
}

