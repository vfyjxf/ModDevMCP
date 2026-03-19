package dev.vfyjxf.moddev.runtime.command;

public record CommandSuggestionQuery(
        String input,
        int cursor,
        int limit
) {
}

