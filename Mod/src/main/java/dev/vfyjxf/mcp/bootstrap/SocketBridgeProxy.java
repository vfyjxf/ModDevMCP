package dev.vfyjxf.mcp.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class SocketBridgeProxy {

    private SocketBridgeProxy() {
    }

    public static void proxy(InputStream stdin, OutputStream stdout, Socket socket) throws IOException {
        var executor = Executors.newFixedThreadPool(2, runnable -> {
            var thread = new Thread(runnable, "moddevmcp-bridge-proxy");
            thread.setDaemon(true);
            return thread;
        });
        try (executor; socket) {
            Future<?> outbound = executor.submit(() -> copyStdinToSocket(stdin, socket));
            Future<?> inbound = executor.submit(() -> copySocketToStdout(socket, stdout));
            waitFor(outbound);
            waitFor(inbound);
        }
    }

    private static void copyStdinToSocket(InputStream stdin, Socket socket) {
        try {
            stdin.transferTo(socket.getOutputStream());
            socket.getOutputStream().flush();
            socket.shutdownOutput();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void copySocketToStdout(Socket socket, OutputStream stdout) {
        try {
            socket.getInputStream().transferTo(stdout);
            stdout.flush();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void waitFor(Future<?> future) throws IOException {
        try {
            future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while proxying bridge traffic", exception);
        } catch (ExecutionException exception) {
            var cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Bridge proxy failed", cause);
        }
    }
}
