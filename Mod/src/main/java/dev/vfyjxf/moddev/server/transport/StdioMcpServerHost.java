package dev.vfyjxf.moddev.server.transport;

import dev.vfyjxf.moddev.server.protocol.McpProtocolDispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class StdioMcpServerHost implements McpServerTransport {

    private static final String DEBUG_LOG_PROPERTY = "moddevmcp.stdio.debugLog";

    private final InputStream input;
    private final OutputStream output;
    private final JsonRpcRequestHandler handler;
    private final JsonCodec jsonCodec;

    public StdioMcpServerHost(InputStream input, OutputStream output, McpProtocolDispatcher dispatcher) {
        this(input, output, new JsonRpcRequestHandler() {
            @Override
            public java.util.Optional<java.util.Map<String, Object>> handle(java.util.Map<String, Object> request) {
                return dispatcher.handle(request);
            }

            @Override
            public java.util.Map<String, Object> initializedNotification() {
                return dispatcher.initializedNotification();
            }
        }, new JsonCodec());
    }

    public StdioMcpServerHost(InputStream input, OutputStream output, JsonRpcRequestHandler handler) {
        this(input, output, handler, new JsonCodec());
    }

    StdioMcpServerHost(InputStream input, OutputStream output, JsonRpcRequestHandler handler, JsonCodec jsonCodec) {
        this.input = input;
        this.output = output;
        this.handler = handler;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void serve() {
        try {
            while (true) {
                var message = readMessage();
                if (message == null) {
                    return;
                }
                debugLog("in", message.body());
                var request = jsonCodec.parseObject(message.body());
                var response = handler.handle(request);
                if (response.isPresent()) {
                    writeMessage(response.get(), message.framing());
                    continue;
                }
                if ("notifications/initialized".equals(request.get("method"))) {
                    writeMessage(handler.initializedNotification(), message.framing());
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private IncomingMessage readMessage() throws IOException {
        while (true) {
            var headers = new LinkedHashMap<String, String>();
            var rawHeaderLines = new ArrayList<String>();
            String line;
            do {
                line = readHeaderLine();
                if (line == null) {
                    if (headers.isEmpty() && rawHeaderLines.isEmpty()) {
                        return null;
                    }
                    debugEvent("eof-before-frame", rawHeaderLines);
                    return null;
                }
                rawHeaderLines.add(line);
                if (!line.isEmpty()) {
                    if (headers.isEmpty() && rawHeaderLines.size() == 1 && looksLikeJsonMessage(line)) {
                        return new IncomingMessage(line.getBytes(StandardCharsets.UTF_8), MessageFraming.JSON_LINE);
                    }
                    var separator = line.indexOf(':');
                    if (separator > 0) {
                        headers.put(
                                line.substring(0, separator).trim().toLowerCase(java.util.Locale.ROOT),
                                line.substring(separator + 1).trim()
                        );
                    }
                }
            } while (!line.isEmpty());
            if (headers.isEmpty()) {
                debugEvent("ignored-empty-frame", rawHeaderLines);
                continue;
            }
            var contentLengthValue = headers.get("content-length");
            if (contentLengthValue == null || contentLengthValue.isBlank()) {
                debugEvent("missing-content-length", rawHeaderLines);
                continue;
            }
            var contentLength = Integer.parseInt(contentLengthValue);
            var body = input.readNBytes(contentLength);
            if (body.length != contentLength) {
                throw new IOException("Unexpected end of stream while reading message body");
            }
            return new IncomingMessage(body, MessageFraming.CONTENT_LENGTH);
        }
    }

    private String readHeaderLine() throws IOException {
        var buffer = new StringBuilder();
        while (true) {
            int next = input.read();
            if (next < 0) {
                return buffer.isEmpty() ? null : buffer.toString();
            }
            if (next == '\n') {
                return buffer.toString();
            }
            if (next == '\r') {
                int lineFeed = input.read();
                if (lineFeed == '\n') {
                    return buffer.toString();
                }
                if (lineFeed >= 0) {
                    throw new IOException("Expected LF after CR in stdio frame");
                }
                return buffer.toString();
            }
            buffer.append((char) next);
        }
    }

    private boolean looksLikeJsonMessage(String line) {
        var trimmed = line.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private void writeMessage(java.util.Map<String, Object> response, MessageFraming framing) throws IOException {
        var body = jsonCodec.write(response);
        debugLog("out", body);
        if (framing == MessageFraming.JSON_LINE) {
            output.write(body);
            output.write('\n');
            output.flush();
            return;
        }
        var header = "Content-Length: " + body.length + "\r\n\r\n";
        output.write(header.getBytes(StandardCharsets.UTF_8));
        output.write(body);
        output.flush();
    }

    private void debugLog(String direction, byte[] body) {
        var configuredPath = System.getProperty(DEBUG_LOG_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            return;
        }
        var path = Path.of(configuredPath);
        var line = "direction=" + direction + " body=" + new String(body, StandardCharsets.UTF_8) + System.lineSeparator();
        writeDebugLine(path, line);
    }

    private void debugEvent(String event, java.util.List<String> rawHeaderLines) {
        var configuredPath = System.getProperty(DEBUG_LOG_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            return;
        }
        var path = Path.of(configuredPath);
        var line = "event=" + event + " rawHeaderLines=" + rawHeaderLines + System.lineSeparator();
        writeDebugLine(path, line);
    }

    private void writeDebugLine(Path path, String line) {
        try {
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private record IncomingMessage(byte[] body, MessageFraming framing) {
    }

    private enum MessageFraming {
        CONTENT_LENGTH,
        JSON_LINE
    }
}

