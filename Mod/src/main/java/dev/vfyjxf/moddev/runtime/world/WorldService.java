package dev.vfyjxf.moddev.runtime.world;

public interface WorldService {

    WorldListResult listWorlds();

    WorldCreateResult createWorld(WorldCreateRequest request);

    WorldJoinResult joinWorld(WorldJoinRequest request);
}

