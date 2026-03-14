package dev.vfyjxf.mcp.runtime.host;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.transport.JsonCodec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HostGameClient implements AutoCloseable {

    private final ModDevMcpServer server;
    private final HostRuntimeClientConfig config;
    private final String runtimeId;
    private final String runtimeSide;
    private final JsonCodec jsonCodec;
    private volatile Socket socket;
    private volatile boolean closed;

    public HostGameClient(ModDevMcpServer server, HostRuntimeClientConfig config, String runtimeId, String runtimeSide) {
        this.server = Objects.requireNonNull(server, "server");
        this.config = Objects.requireNonNull(config, "config");
        this.runtimeId = Objects.requireNonNull(runtimeId, "runtimeId");
        this.runtimeSide = Objects.requireNonNull(runtimeSide, "runtimeSide");
        this.jsonCodec = new JsonCodec();
    }

    public void runUntilDisconnected() throws IOException {
        ModDevMCP.LOGGER.info("Connecting runtime {} to host {}:{}", runtimeId, config.host(), config.port());
        try (var connectedSocket = new Socket(config.host(), config.port())) {
            socket = connectedSocket;
            ModDevMCP.LOGGER.info("Runtime {} connected to host {}:{}", runtimeId, config.host(), config.port());
            try (var reader = new BufferedReader(new InputStreamReader(connectedSocket.getInputStream(), StandardCharsets.UTF_8));
                 var writer = new BufferedWriter(new OutputStreamWriter(connectedSocket.getOutputStream(), StandardCharsets.UTF_8))) {
                write(writer, Map.of(
                        "type", "runtime.hello",
                        "runtimeId", runtimeId,
                        "runtimeSide", runtimeSide,
                        "supportedScopes", supportedScopes(),
                        "supportedSides", List.of(runtimeSide),
                        "toolDescriptors", new GameRuntimeDescriptorFactory(server, runtimeSide).createToolDescriptors(),
                        "state", Map.of()
                ));
                String line;
                while (!closed && (line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    handleMessage(writer, jsonCodec.parseObject(line.getBytes(StandardCharsets.UTF_8)));
                }
            } finally {
                socket = null;
                ModDevMCP.LOGGER.info("Runtime {} disconnected from host", runtimeId);
            }
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (socket != null) {
            socket.close();
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(BufferedWriter writer, Map<String, Object> message) throws IOException {
        if (!"runtime.call".equals(message.get("type"))) {
            return;
        }
        var callId = message.get("callId") instanceof String value ? value : "";
        var toolName = message.get("toolName") instanceof String value ? value : "";
        var arguments = message.get("arguments") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.<String, Object>of();
        var tool = server.registry().findTool(toolName);
        ToolResult result = tool
                .map(registeredTool -> registeredTool.handler().handle(new ToolCallContext(runtimeSide, Map.of("runtimeId", runtimeId)), Map.copyOf(arguments)))
                .orElseGet(() -> ToolResult.failure("tool_not_found"));
        if (result.success()) {
            write(writer, Map.of(
                    "type", "runtime.result",
                    "callId", callId,
                    "success", true,
                    "value", result.value()
            ));
            return;
        }
        write(writer, Map.of(
                "type", "runtime.result",
                "callId", callId,
                "success", false,
                "error", result.error() == null ? "runtime_protocol_error" : result.error()
        ));
    }

    private List<String> supportedScopes() {
        return switch (runtimeSide) {
            case "client" -> List.of("common", "client");
            case "server" -> List.of("common", "server");
            default -> List.of("common");
        };
    }

    private void write(BufferedWriter writer, Map<String, Object> payload) throws IOException {
        writer.write(jsonCodec.writeString(payload));
        writer.newLine();
        writer.flush();
    }
}
