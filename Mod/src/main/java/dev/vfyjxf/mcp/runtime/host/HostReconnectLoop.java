package dev.vfyjxf.mcp.runtime.host;

import dev.vfyjxf.mcp.ModDevMCP;

import java.util.Objects;
import java.util.function.Consumer;

public final class HostReconnectLoop implements AutoCloseable {

    @FunctionalInterface
    public interface Connector {
        void runOnce() throws Exception;
    }

    private final Connector connector;
    private final long reconnectDelayMs;
    private final Consumer<String> logSink;
    private final Thread worker;
    private volatile boolean closed;

    public HostReconnectLoop(Connector connector, long reconnectDelayMs) {
        this(connector, reconnectDelayMs, message -> ModDevMCP.LOGGER.info(message));
    }

    HostReconnectLoop(Connector connector, long reconnectDelayMs, Consumer<String> logSink) {
        this.connector = Objects.requireNonNull(connector, "connector");
        this.reconnectDelayMs = reconnectDelayMs;
        this.logSink = Objects.requireNonNull(logSink, "logSink");
        this.worker = new Thread(this::loop, "moddev-runtime-host-reconnect");
        this.worker.setDaemon(true);
    }

    public void start() {
        worker.start();
    }

    @Override
    public void close() {
        closed = true;
        worker.interrupt();
        try {
            worker.join(1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void loop() {
        while (!closed) {
            try {
                connector.runOnce();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ignored) {
            }
            if (closed) {
                return;
            }
            logSink.accept("Runtime gateway disconnected; reconnecting in " + reconnectDelayMs + " ms");
            try {
                Thread.sleep(reconnectDelayMs);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
