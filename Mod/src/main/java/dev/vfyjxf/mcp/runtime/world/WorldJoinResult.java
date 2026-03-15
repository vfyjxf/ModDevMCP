package dev.vfyjxf.mcp.runtime.world;

public record WorldJoinResult(
        String worldId,
        String worldName,
        boolean joined
) {
}
