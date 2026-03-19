package dev.vfyjxf.moddev.runtime.game;

import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class LiveClientGameCloser implements GameCloser {

    private static final long EXECUTION_TIMEOUT_SECONDS = 5L;

    @Override
    public boolean requestClose() {
        try {
            var minecraft = Minecraft.getInstance();
            if (minecraft.isSameThread()) {
                return requestCloseOnClient(minecraft);
            }
            var future = new CompletableFuture<Boolean>();
            minecraft.execute(() -> future.complete(requestCloseOnClient(minecraft)));
            return future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException exception) {
            return false;
        }
    }

    private boolean requestCloseOnClient(Minecraft minecraft) {
        minecraft.stop();
        return true;
    }
}

