package dev.vfyjxf.mcp.server.host;

import java.util.Map;

public record RuntimeToolSelection(
        RuntimeSession session,
        RuntimeToolDescriptor descriptor,
        String routingKey,
        String error
) {
    public static RuntimeToolSelection resolved(RuntimeSession session, RuntimeToolDescriptor descriptor) {
        return resolved(session, descriptor, null);
    }

    public static RuntimeToolSelection resolved(RuntimeSession session, RuntimeToolDescriptor descriptor, String routingKey) {
        return new RuntimeToolSelection(session, descriptor, routingKey, null);
    }

    public static RuntimeToolSelection failure(String error) {
        return new RuntimeToolSelection(null, null, null, error);
    }

    public boolean resolved() {
        return error == null;
    }

    public Map<String, Object> argumentsWithoutRoutingKeys(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty() || routingKey == null || !arguments.containsKey(routingKey)) {
            return arguments == null ? Map.of() : Map.copyOf(arguments);
        }
        var copy = new java.util.LinkedHashMap<String, Object>(arguments);
        copy.remove(routingKey);
        return Map.copyOf(copy);
    }
}
