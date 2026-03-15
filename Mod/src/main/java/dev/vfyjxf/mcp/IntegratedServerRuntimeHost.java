package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.runtime.host.HostGameClient;
import dev.vfyjxf.mcp.runtime.host.HostReconnectLoop;
import dev.vfyjxf.mcp.runtime.host.HostRuntimeClientConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.Objects;

public final class IntegratedServerRuntimeHost implements AutoCloseable {

    private final Runnable startAction;
    private final Runnable stopAction;
    private HostReconnectLoop reconnectLoop;
    private boolean running;

    public IntegratedServerRuntimeHost(HostRuntimeClientConfig config) {
        this(new ServerRuntimeBootstrap(new ModDevMCP()), config, null, null);
    }

    IntegratedServerRuntimeHost(ServerRuntimeBootstrap serverBootstrap, HostRuntimeClientConfig config) {
        this(serverBootstrap, config, null, null);
    }

    IntegratedServerRuntimeHost(Runnable startAction, Runnable stopAction) {
        this(null, null, startAction, stopAction);
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

    private IntegratedServerRuntimeHost(
            ServerRuntimeBootstrap serverBootstrap,
            HostRuntimeClientConfig config,
            Runnable startAction,
            Runnable stopAction
    ) {
        if (startAction != null && stopAction != null) {
            this.startAction = Objects.requireNonNull(startAction, "startAction");
            this.stopAction = Objects.requireNonNull(stopAction, "stopAction");
            return;
        }
        var resolvedBootstrap = Objects.requireNonNull(serverBootstrap, "serverBootstrap");
        var resolvedConfig = Objects.requireNonNull(config, "config");
        this.startAction = () -> startLoop(resolvedBootstrap, resolvedConfig);
        this.stopAction = this::stopLoop;
    }

    private void startLoop(ServerRuntimeBootstrap serverBootstrap, HostRuntimeClientConfig config) {
        var loop = new HostReconnectLoop(
                () -> new HostGameClient(serverBootstrap.prepareServer(), config, "integrated-server-runtime", "server").runUntilDisconnected(),
                config.reconnectDelayMs()
        );
        reconnectLoop = loop;
        loop.start();
    }

    private void stopLoop() {
        var loop = reconnectLoop;
        reconnectLoop = null;
        if (loop != null) {
            loop.close();
        }
    }
}
