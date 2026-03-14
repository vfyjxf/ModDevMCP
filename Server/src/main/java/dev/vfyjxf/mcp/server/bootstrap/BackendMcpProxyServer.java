package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.protocol.McpProtocolDispatcher;
import dev.vfyjxf.mcp.server.transport.JsonCodec;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class BackendMcpProxyServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final McpProtocolDispatcher dispatcher;
    private final JsonCodec jsonCodec;
    private final java.util.Set<Socket> openSockets;
    private final Thread acceptThread;
    private volatile boolean closed;

    private BackendMcpProxyServer(ServerSocket serverSocket, ModDevMcpServer server) {
        this.serverSocket = Objects.requireNonNull(serverSocket, "serverSocket");
        this.dispatcher = ModDevMcpServerFactory.createDispatcher(server);
        this.jsonCodec = new JsonCodec();
        this.openSockets = ConcurrentHashMap.newKeySet();
        this.acceptThread = new Thread(this::acceptLoop, "moddev-backend-mcp-accept");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    public static BackendMcpProxyServer start(ModDevMcpServer server, String host, int port) throws IOException {
        return new BackendMcpProxyServer(new ServerSocket(port, 50, InetAddress.getByName(host)), server);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        for (var socket : openSockets) {
            socket.close();
        }
        serverSocket.close();
        try {
            acceptThread.join(1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                var socket = serverSocket.accept();
                openSockets.add(socket);
                var thread = new Thread(() -> handleConnection(socket), "moddev-backend-mcp-client");
                thread.setDaemon(true);
                thread.start();
            } catch (IOException exception) {
                if (!closed) {
                    throw new RuntimeException("Failed to accept backend MCP connection", exception);
                }
                return;
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                var request = jsonCodec.parseObject(line.getBytes(StandardCharsets.UTF_8));
                var response = dispatcher.handle(request);
                if (response.isPresent()) {
                    write(writer, response.get());
                    continue;
                }
                if ("notifications/initialized".equals(request.get("method"))) {
                    write(writer, dispatcher.initializedNotification());
                }
            }
        } catch (IOException exception) {
            if (!closed) {
                throw new RuntimeException("Failed to process backend MCP connection", exception);
            }
        } finally {
            openSockets.remove(socket);
        }
    }

    private void write(BufferedWriter writer, Map<String, Object> payload) throws IOException {
        writer.write(jsonCodec.writeString(payload));
        writer.newLine();
        writer.flush();
    }
}
