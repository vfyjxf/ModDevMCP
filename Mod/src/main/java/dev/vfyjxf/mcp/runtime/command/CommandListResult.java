package dev.vfyjxf.mcp.runtime.command;

import java.util.List;

public record CommandListResult(
        List<CommandDescriptor> commands,
        int totalMatched,
        boolean truncated
) {
    public CommandListResult {
        commands = commands == null ? List.of() : List.copyOf(commands);
    }
}
