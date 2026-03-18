package dev.vfyjxf.mcp.server.transport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GsonMcpJsonMapper implements McpJsonMapper {

    private final Gson gson;

    public GsonMcpJsonMapper() {
        this(new GsonBuilder()
                .serializeNulls()
                .disableHtmlEscaping()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .create());
    }

    GsonMcpJsonMapper(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> T readValue(String value, Class<T> type) throws IOException {
        return gson.fromJson(value, type);
    }

    @Override
    public <T> T readValue(byte[] value, Class<T> type) throws IOException {
        return readValue(new String(value, StandardCharsets.UTF_8), type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(String value, TypeRef<T> type) throws IOException {
        return (T) normalizeContainers(gson.fromJson(value, type.getType()));
    }

    @Override
    public <T> T readValue(byte[] value, TypeRef<T> type) throws IOException {
        return readValue(new String(value, StandardCharsets.UTF_8), type);
    }

    @Override
    public <T> T convertValue(Object value, Class<T> type) {
        return gson.fromJson(gson.toJsonTree(value), type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object value, TypeRef<T> type) {
        return (T) normalizeContainers(gson.fromJson(gson.toJsonTree(value), type.getType()));
    }

    @Override
    public String writeValueAsString(Object value) throws IOException {
        return gson.toJson(value);
    }

    @Override
    public byte[] writeValueAsBytes(Object value) throws IOException {
        return writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
    }

    private Object normalizeContainers(Object value) {
        if (value instanceof Map<?, ?> map) {
            var normalized = new HashMap<String, Object>();
            for (var entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), normalizeContainers(entry.getValue()));
            }
            return normalized;
        }
        if (value instanceof List<?> list) {
            var normalized = new ArrayList<Object>(list.size());
            for (var item : list) {
                normalized.add(normalizeContainers(item));
            }
            return normalized;
        }
        return value;
    }
}
