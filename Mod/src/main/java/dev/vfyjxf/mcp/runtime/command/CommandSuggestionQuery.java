package dev.vfyjxf.mcp.runtime.command;

public record CommandSuggestionQuery(
        String input,
        int cursor,
        int limit
) {
}
