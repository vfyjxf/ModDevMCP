package dev.vfyjxf.moddev.server.bootstrap;

import dev.vfyjxf.moddev.server.transport.JsonCodec;
import dev.vfyjxf.moddev.server.transport.JsonRpcRequestHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public final class BackendProxyJsonRpcHandler implements JsonRpcRequestHandler, AutoCloseable {

    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final JsonCodec jsonCodec;

    public BackendProxyJsonRpcHandler(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.jsonCodec = new JsonCodec();
    }

    @Override
    public synchronized Optional<Map<String, Object>> handle(Map<String, Object> request) {
        if ("notifications/initialized".equals(request.get("method"))) {
            return Optional.empty();
        }
        try {
            return Optional.of(sendAndReceive(request));
        } catch (IOException exception) {
            throw new IllegalStateException("backend_unavailable", exception);
        }
    }

    @Override
    public synchronized Map<String, Object> initializedNotification() {
        try {
            return sendAndReceive(Map.of(
                    "jsonrpc", "2.0",
                    "method", "notifications/initialized"
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("backend_unavailable", exception);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private Map<String, Object> sendAndReceive(Map<String, Object> payload) throws IOException {
        writer.write(jsonCodec.writeString(payload));
        writer.newLine();
        writer.flush();
        var line = reader.readLine();
        if (line == null || line.isBlank()) {
            throw new IOException("Backend closed connection");
        }
        return jsonCodec.parseObject(line.getBytes(StandardCharsets.UTF_8));
    }
}

