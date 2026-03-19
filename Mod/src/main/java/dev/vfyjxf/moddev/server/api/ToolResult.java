package dev.vfyjxf.moddev.server.api;

public record ToolResult(
        boolean success,
        Object value,
        String error
) {
    public static ToolResult success(Object value) {
        return new ToolResult(true, value, null);
    }

    public static ToolResult failure(String error) {
        return new ToolResult(false, null, error);
    }
}

