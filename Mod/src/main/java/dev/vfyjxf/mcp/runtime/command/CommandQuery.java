package dev.vfyjxf.mcp.runtime.command;

public record CommandQuery(
        String query,
        int limit
) {
}
