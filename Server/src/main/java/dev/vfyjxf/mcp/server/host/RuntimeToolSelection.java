package dev.vfyjxf.mcp.server.host;

import java.util.Map;

public record RuntimeToolSelection(
        RuntimeSession session,
        RuntimeToolDescriptor descriptor,
        String error
) {
    public static RuntimeToolSelection resolved(RuntimeSession session, RuntimeToolDescriptor descriptor) {
        return new RuntimeToolSelection(session, descriptor, null);
    }

    public static RuntimeToolSelection failure(String error) {
        return new RuntimeToolSelection(null, null, error);
    }

    public boolean resolved() {
        return error == null;
    }

    public Map<String, Object> argumentsWithoutRoutingKeys(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty() || !arguments.containsKey("targetSide")) {
            return arguments == null ? Map.of() : Map.copyOf(arguments);
        }
        var copy = new java.util.LinkedHashMap<String, Object>(arguments);
        copy.remove("targetSide");
        return Map.copyOf(copy);
    }
}
