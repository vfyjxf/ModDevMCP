package dev.vfyjxf.moddev.runtime.world;

public record WorldCreateResult(
        String worldId,
        String worldName,
        boolean created,
        boolean joined
) {
}

