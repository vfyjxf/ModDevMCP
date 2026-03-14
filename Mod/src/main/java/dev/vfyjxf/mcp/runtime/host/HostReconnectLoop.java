package dev.vfyjxf.mcp.runtime.host;

import java.util.Objects;

public final class HostReconnectLoop implements AutoCloseable {

    @FunctionalInterface
    public interface Connector {
        void runOnce() throws Exception;
    }

    private final Connector connector;
    private final long reconnectDelayMs;
    private final Thread worker;
    private volatile boolean closed;

    public HostReconnectLoop(Connector connector, long reconnectDelayMs) {
        this.connector = Objects.requireNonNull(connector, "connector");
        this.reconnectDelayMs = reconnectDelayMs;
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
            try {
                Thread.sleep(reconnectDelayMs);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}


