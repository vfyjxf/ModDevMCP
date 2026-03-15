package dev.vfyjxf.mcp.runtime.world;

public record WorldCreateRequest(
        String name,
        String gameMode,
        boolean allowCheats,
        String seed,
        String worldType,
        String difficulty,
        boolean bonusChest,
        boolean generateStructures,
        boolean joinAfterCreate
) {
}
