package dev.vfyjxf.mcp.runtime.world;

public record WorldCreateResult(
        String worldId,
        String worldName,
        boolean created,
        boolean joined
) {
}
