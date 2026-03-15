package dev.vfyjxf.mcp.runtime.command;

import java.util.List;

public record CommandExecutionResult(
        String normalizedCommand,
        boolean executed,
        Integer resultCode,
        List<String> messages,
        String errorCode,
        String errorDetail
) {
    public CommandExecutionResult {
        normalizedCommand = normalizedCommand == null ? "" : normalizedCommand;
        messages = messages == null ? List.of() : List.copyOf(messages);
        errorCode = errorCode == null ? "" : errorCode;
        errorDetail = errorDetail == null ? "" : errorDetail;
    }

    public static CommandExecutionResult success(String normalizedCommand, int resultCode, List<String> messages) {
        return new CommandExecutionResult(normalizedCommand, true, resultCode, messages, "", "");
    }

    public static CommandExecutionResult failure(String normalizedCommand, String errorCode, String errorDetail, List<String> messages) {
        return new CommandExecutionResult(normalizedCommand, false, null, messages, errorCode, errorDetail);
    }
}
