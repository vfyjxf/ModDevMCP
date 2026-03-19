package dev.vfyjxf.moddev.runtime.command;

public record CommandDescriptor(
        String name,
        String usage,
        String source,
        String namespace,
        String summary,
        CommandType type
) {
}

