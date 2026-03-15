package dev.vfyjxf.mcp.runtime.world;

public record WorldDescriptor(
        String id,
        String name,
        long lastPlayed,
        String gameMode,
        boolean hardcore,
        boolean cheatsKnown
) {
}
