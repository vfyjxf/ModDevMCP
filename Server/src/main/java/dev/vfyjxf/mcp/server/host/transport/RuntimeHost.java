package dev.vfyjxf.mcp.server.host.transport;

import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.host.RuntimeCallQueue;
import dev.vfyjxf.mcp.server.host.RuntimeInvoker;
import dev.vfyjxf.mcp.server.host.RuntimeRegistry;
import dev.vfyjxf.mcp.server.host.RuntimeSession;
import dev.vfyjxf.mcp.server.host.RuntimeToolDescriptor;
import dev.vfyjxf.mcp.server.host.protocol.RuntimeHostDispatcher;
import dev.vfyjxf.mcp.server.transport.JsonCodec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class RuntimeHost implements AutoCloseable, RuntimeInvoker {

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(10);

    private final ServerSocket serverSocket;
    private final RuntimeHostDispatcher dispatcher;
    private final JsonCodec jsonCodec;
    private final Set<Socket> openSockets;
    private final ConcurrentHashMap<String, CompletableFuture<ToolResult>> pendingResults;
    private final ConcurrentHashMap<String, RuntimeConnection> connectionsByRuntimeId;
    private final AtomicLong nextCallId;
    private final RuntimeCallQueue scheduler;
    private final Thread acceptThread;
    private volatile boolean closed;

    private RuntimeHost(ServerSocket serverSocket, RuntimeRegistry registry, RuntimeCallQueue scheduler) {
        this.serverSocket = Objects.requireNonNull(serverSocket, "serverSocket");
        this.dispatcher = new RuntimeHostDispatcher(Objects.requireNonNull(registry, "registry"));
        this.jsonCodec = new JsonCodec();
        this.openSockets = ConcurrentHashMap.newKeySet();
        this.pendingResults = new ConcurrentHashMap<>();
        this.connectionsByRuntimeId = new ConcurrentHashMap<>();
        this.nextCallId = new AtomicLong();
        this.scheduler = scheduler;
        if (scheduler != null) {
            scheduler.setInvoker(this);
        }
        this.acceptThread = new Thread(this::acceptLoop, "moddev-runtime-host-accept");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    public static RuntimeHost start(RuntimeRegistry registry, String host, int port) throws IOException {
        return start(registry, host, port, null);
    }

    public static RuntimeHost start(RuntimeRegistry registry, String host, int port, RuntimeCallQueue scheduler) throws IOException {
        var serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
        return new RuntimeHost(serverSocket, registry, scheduler);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    @Override
    public ToolResult invoke(RuntimeSession session, RuntimeToolDescriptor descriptor, Map<String, Object> arguments) throws Exception {
        var connection = connectionsByRuntimeId.get(session.runtimeId());
        if (connection == null || connection.runtimeId() == null || !connection.runtimeId().equals(session.runtimeId())) {
            return ToolResult.failure("game_not_connected");
        }
        var callId = "call-" + nextCallId.incrementAndGet();
        var future = new CompletableFuture<ToolResult>();
        pendingResults.put(callId, future);
        try {
            connection.write(Map.of(
                    "type", "runtime.call",
                    "callId", callId,
                    "runtimeId", session.runtimeId(),
                    "toolName", descriptor.definition().name(),
                    "arguments", Map.copyOf(arguments)
            ));
            return future.get(CALL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException exception) {
            return ToolResult.failure("game_call_timeout");
        } finally {
            pendingResults.remove(callId);
        }
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
                var thread = new Thread(() -> handleConnection(socket), "moddev-runtime-host-client");
                thread.setDaemon(true);
                thread.start();
            } catch (IOException exception) {
                if (!closed) {
                    throw new RuntimeException("Failed to accept runtime host connection", exception);
                }
                return;
            }
        }
    }

    private void handleConnection(Socket socket) {
        String runtimeId = null;
        RuntimeConnection connection = null;
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            connection = new RuntimeConnection(socket, writer);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                var request = jsonCodec.parseObject(line.getBytes(StandardCharsets.UTF_8));
                if ("runtime.result".equals(request.get("type"))) {
                    handleRuntimeResult(request);
                    continue;
                }
                if (request.get("runtimeId") instanceof String requestRuntimeId && !requestRuntimeId.isBlank()) {
                    runtimeId = requestRuntimeId;
                    if ("runtime.hello".equals(request.get("type"))) {
                        connection.runtimeId = runtimeId;
                        connectionsByRuntimeId.put(runtimeId, connection);
                        System.err.println("runtime connected: " + runtimeId + " from " + socket.getRemoteSocketAddress());
                    }
                }
                var response = dispatcher.handle(request);
                connection.write(response);
            }
        } catch (IOException exception) {
            if (!closed) {
                throw new RuntimeException("Failed to process runtime host connection", exception);
            }
        } finally {
            openSockets.remove(socket);
            if (connection != null && connection.runtimeId() != null) {
                connectionsByRuntimeId.remove(connection.runtimeId(), connection);
            }
            if (runtimeId != null && !runtimeId.isBlank()) {
                dispatcher.handle(java.util.Map.of(
                        "type", "runtime.goodbye",
                        "runtimeId", runtimeId
                ));
                pendingResults.forEach((callId, future) -> future.complete(ToolResult.failure("game_disconnected")));
                if (scheduler != null) {
                    scheduler.onRuntimeDisconnected(runtimeId);
                }
                System.err.println("runtime disconnected: " + runtimeId);
            }
        }
    }

    private void handleRuntimeResult(Map<String, Object> request) {
        var callId = request.get("callId") instanceof String value && !value.isBlank() ? value : null;
        if (callId == null) {
            return;
        }
        var future = pendingResults.remove(callId);
        if (future == null) {
            return;
        }
        if (Boolean.TRUE.equals(request.get("success"))) {
            future.complete(ToolResult.success(request.get("value")));
            return;
        }
        var error = request.get("error") instanceof String value && !value.isBlank()
                ? value
                : "runtime_protocol_error";
        future.complete(ToolResult.failure(error));
    }

    private final class RuntimeConnection {
        private final Socket socket;
        private final BufferedWriter writer;
        private volatile String runtimeId;

        private RuntimeConnection(Socket socket, BufferedWriter writer) {
            this.socket = socket;
            this.writer = writer;
        }

        private String runtimeId() {
            return runtimeId;
        }

        private synchronized void write(Map<String, Object> payload) throws IOException {
            writer.write(jsonCodec.writeString(payload));
            writer.newLine();
            writer.flush();
        }
    }
}
