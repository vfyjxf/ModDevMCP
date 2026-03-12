package dev.vfyjxf.mcp.server.transport;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.protocol.McpProtocolDispatcher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SocketMcpServerHost implements McpServerTransport, AutoCloseable {

    private final ModDevMcpServer server;
    private final ServerSocket serverSocket;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Thread acceptThread;

    private SocketMcpServerHost(ModDevMcpServer server, ServerSocket serverSocket) {
        this.server = server;
        this.serverSocket = serverSocket;
        this.acceptThread = new Thread(this::acceptLoop, "moddev-mcp-socket-host");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    public static SocketMcpServerHost start(ModDevMcpServer server, String host, int port) throws IOException {
        var serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(host, port));
        return new SocketMcpServerHost(server, serverSocket);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void serve() {
        try {
            acceptThread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while serving MCP socket host", exception);
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        IOException failure = null;
        try {
            serverSocket.close();
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            acceptThread.join(1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            var interrupted = new IOException("Interrupted while stopping MCP socket host", exception);
            if (failure == null) {
                failure = interrupted;
            } else {
                failure.addSuppressed(interrupted);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void acceptLoop() {
        while (!closed.get()) {
            try {
                var socket = serverSocket.accept();
                var worker = new Thread(() -> handleClient(socket), "moddev-mcp-socket-client");
                worker.setDaemon(true);
                worker.start();
            } catch (SocketException exception) {
                if (!closed.get()) {
                    throw new IllegalStateException("MCP socket host stopped unexpectedly", exception);
                }
                return;
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to accept MCP socket client", exception);
            }
        }
    }

    private void handleClient(java.net.Socket socket) {
        try (socket) {
            new StdioMcpServerHost(
                    socket.getInputStream(),
                    socket.getOutputStream(),
                    new McpProtocolDispatcher(server)
            ).serve();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serve MCP socket client", exception);
        }
    }
}
