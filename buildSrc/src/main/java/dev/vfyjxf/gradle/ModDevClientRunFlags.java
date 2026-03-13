package dev.vfyjxf.gradle;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModDevClientRunFlags {
    public static final String DEV_UI_CAPTURE_PROPERTY = "moddevmcp.devUiCapture";
    public static final String MCP_HOST_PROPERTY = "moddevmcp.host";
    public static final String MCP_PORT_PROPERTY = "moddevmcp.port";

    private ModDevClientRunFlags() {
    }

    public static Map<String, String> resolveJvmSystemProperties(
            Map<String, String> systemProperties,
            Map<String, String> gradleProperties
    ) {
        var resolved = new LinkedHashMap<String, String>();
        putIfPresent(resolved, DEV_UI_CAPTURE_PROPERTY, systemProperties.get(DEV_UI_CAPTURE_PROPERTY));
        if (!resolved.containsKey(DEV_UI_CAPTURE_PROPERTY)) {
            putIfPresent(resolved, DEV_UI_CAPTURE_PROPERTY, gradleProperties.get(DEV_UI_CAPTURE_PROPERTY));
        }
        putResolved(resolved, MCP_HOST_PROPERTY, systemProperties, gradleProperties);
        putResolved(resolved, MCP_PORT_PROPERTY, systemProperties, gradleProperties);
        return Map.copyOf(resolved);
    }

    private static void putResolved(
            Map<String, String> target,
            String key,
            Map<String, String> systemProperties,
            Map<String, String> gradleProperties
    ) {
        putIfPresent(target, key, systemProperties.get(key));
        if (!target.containsKey(key)) {
            putIfPresent(target, key, gradleProperties.get(key));
        }
    }

    private static void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }
}
