package dev.vfyjxf.moddev.server.transport;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator;

import java.io.IOException;
import java.util.Map;

public final class PermissiveJsonSchemaValidator implements JsonSchemaValidator {

    public static final PermissiveJsonSchemaValidator INSTANCE = new PermissiveJsonSchemaValidator();

    private static final GsonMcpJsonMapper JSON_MAPPER = new GsonMcpJsonMapper();

    private PermissiveJsonSchemaValidator() {
    }

    @Override
    public ValidationResponse validate(Map<String, Object> schema, Object value) {
        try {
            return ValidationResponse.asValid(JSON_MAPPER.writeValueAsString(value));
        } catch (IOException exception) {
            return ValidationResponse.asValid(String.valueOf(value));
        }
    }
}

