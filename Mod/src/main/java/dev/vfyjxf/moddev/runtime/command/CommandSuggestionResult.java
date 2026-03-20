package dev.vfyjxf.moddev.runtime.command;

import java.util.List;

public record CommandSuggestionResult(
        String normalizedInput,
        int parseValidUpTo,
        List<CommandSuggestion> suggestions
) {
    public CommandSuggestionResult {
        normalizedInput = normalizedInput == null ? "" : normalizedInput;
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }
}

