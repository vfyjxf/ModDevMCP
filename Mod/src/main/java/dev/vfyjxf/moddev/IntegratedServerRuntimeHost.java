package dev.vfyjxf.moddev;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.Objects;

public final class IntegratedServerRuntimeHost implements AutoCloseable {

    private final Runnable startAction;
    private final Runnable stopAction;
    private boolean running;

    IntegratedServerRuntimeHost(Runnable startAction, Runnable stopAction) {
        this.startAction = Objects.requireNonNull(startAction, "startAction");
        this.stopAction = Objects.requireNonNull(stopAction, "stopAction");
    }

    public void attach() {
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    @Override
    public void close() {
        if (!running) {
            return;
        }
        running = false;
        stopAction.run();
    }

    void handleServerStarted(boolean dedicatedServer) {
        if (dedicatedServer || running) {
            return;
        }
        startAction.run();
        running = true;
    }

    void handleServerStopping(boolean dedicatedServer) {
        if (dedicatedServer || !running) {
            return;
        }
        running = false;
        stopAction.run();
    }

    private void onServerStarted(ServerStartedEvent event) {
        handleServerStarted(event.getServer().isDedicatedServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        handleServerStopping(event.getServer().isDedicatedServer());
    }
}

