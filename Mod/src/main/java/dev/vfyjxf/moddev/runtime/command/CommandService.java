package dev.vfyjxf.moddev.runtime.command;

public interface CommandService {

    CommandListResult list(CommandQuery query);

    CommandSuggestionResult suggest(CommandSuggestionQuery query);

    CommandExecutionResult execute(CommandExecutionRequest request);
}

