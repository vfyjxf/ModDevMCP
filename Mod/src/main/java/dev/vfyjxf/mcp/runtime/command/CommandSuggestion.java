package dev.vfyjxf.mcp.runtime.command;

public record CommandSuggestion(
        String text,
        int rangeStart,
        int rangeEnd,
        String tooltip
) {
}
