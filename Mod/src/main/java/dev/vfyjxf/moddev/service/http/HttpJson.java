package dev.vfyjxf.moddev.service.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

final class HttpJson {

    private HttpJson() {
    }

    static void sendJson(HttpExchange exchange, int statusCode, Object value) throws IOException {
        send(exchange, statusCode, "application/json; charset=utf-8", toJson(value));
    }

    static void sendMarkdown(HttpExchange exchange, int statusCode, String markdown) throws IOException {
        send(exchange, statusCode, "text/markdown; charset=utf-8", markdown);
    }

    static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Allow", "GET");
        sendJson(exchange, 405, Map.of("error", "method_not_allowed"));
    }

    static void sendNotFound(HttpExchange exchange) throws IOException {
        sendJson(exchange, 404, Map.of("error", "not_found"));
    }

    static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escape(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> mapValue) {
            var builder = new StringBuilder();
            builder.append('{');
            var iterator = mapValue.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException("JSON map keys must be strings");
                }
                builder.append('"').append(escape(key)).append('"').append(':').append(toJson(entry.getValue()));
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
            return builder.toString();
        }
        if (value instanceof Iterable<?> iterable) {
            var builder = new StringBuilder();
            builder.append('[');
            appendValues(builder, iterable.iterator());
            builder.append(']');
            return builder.toString();
        }
        if (value.getClass().isArray()) {
            var builder = new StringBuilder();
            builder.append('[');
            var length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                builder.append(toJson(java.lang.reflect.Array.get(value, i)));
                if (i + 1 < length) {
                    builder.append(',');
                }
            }
            builder.append(']');
            return builder.toString();
        }
        throw new IllegalArgumentException("unsupported JSON value type: " + value.getClass().getName());
    }

    private static void appendValues(StringBuilder builder, Iterator<?> iterator) {
        while (iterator.hasNext()) {
            builder.append(toJson(iterator.next()));
            if (iterator.hasNext()) {
                builder.append(',');
            }
        }
    }

    private static String escape(String value) {
        var builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            var ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static void send(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}

