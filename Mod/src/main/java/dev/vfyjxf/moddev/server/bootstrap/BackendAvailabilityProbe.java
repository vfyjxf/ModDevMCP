package dev.vfyjxf.moddev.server.bootstrap;

import dev.vfyjxf.moddev.server.transport.JsonCodec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public final class BackendAvailabilityProbe {

    private final Duration connectTimeout;
    private final JsonCodec jsonCodec;

    public BackendAvailabilityProbe(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        this.jsonCodec = new JsonCodec();
    }

    public boolean isReady(String host, int port) {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.toIntExact(connectTimeout.toMillis()));
            socket.setSoTimeout(Math.toIntExact(connectTimeout.toMillis()));
            try (var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(jsonCodec.writeString(Map.of(
                        "jsonrpc", "2.0",
                        "id", "probe",
                        "method", "initialize",
                        "params", Map.of(
                                "protocolVersion", "2025-06-18",
                                "capabilities", Map.of(),
                                "clientInfo", Map.of(
                                        "name", "backend-probe",
                                        "version", "0.0.0"
                                )
                        )
                )));
                writer.newLine();
                writer.flush();
                var line = reader.readLine();
                return line != null && line.contains("\"serverInfo\"");
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}

