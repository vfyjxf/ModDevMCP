package dev.vfyjxf.moddev.server.transport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JsonCodec {

    private static final java.lang.reflect.Type OBJECT_MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private final Gson gson;

    public JsonCodec() {
        this(new GsonBuilder()
                .serializeNulls()
                .disableHtmlEscaping()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .create());
    }

    JsonCodec(Gson gson) {
        this.gson = gson;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseObject(byte[] bytes) {
        var value = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), OBJECT_MAP_TYPE);
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        return (Map<String, Object>) map;
    }

    public byte[] write(Object value) {
        return writeString(value).getBytes(StandardCharsets.UTF_8);
    }

    public String writeString(Object value) {
        return gson.toJson(value);
    }
}

