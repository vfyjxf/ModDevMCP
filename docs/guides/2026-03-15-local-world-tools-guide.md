# Local World Tools Guide

Date: 2026-03-15

## Purpose

Describe the client-runtime MCP tools used to list local worlds, create a local world, and enter a local world through the real Minecraft client.

## Tools

- `moddev.world_list`
- `moddev.world_create`
- `moddev.world_join`

## Scope

- local singleplayer worlds only
- client runtime only
- no multiplayer server support

## Recommended Flow

1. call `moddev.status`
2. confirm `gameConnected=true`
3. call `moddev.world_list`
4. call `moddev.world_join` with `id` when entering an existing world
5. call `moddev.world_create` when you need a fresh local test world

## Minimal Examples

List local worlds:

```json
{
  "name": "moddev.world_list",
  "arguments": {}
}
```

Create and enter a creative test world:

```json
{
  "name": "moddev.world_create",
  "arguments": {
    "name": "MCP Test World",
    "gameMode": "creative",
    "allowCheats": true,
    "worldType": "default",
    "difficulty": "normal",
    "generateStructures": true
  }
}
```

Create and enter a superflat build world:

```json
{
  "name": "moddev.world_create",
  "arguments": {
    "name": "Flat Build World",
    "gameMode": "creative",
    "allowCheats": true,
    "worldType": "flat",
    "difficulty": "peaceful",
    "bonusChest": false,
    "generateStructures": false
  }
}
```

Join an existing local world:

```json
{
  "name": "moddev.world_join",
  "arguments": {
    "id": "New World"
  }
}
```

## Practical Notes

- `world_create` defaults `joinAfterCreate=true`
- `world_create` currently requires `joinAfterCreate=true`
- `world_create` supports `worldType=default|flat|large_biomes|amplified`
- `world_create` supports `difficulty=peaceful|easy|normal|hard`
- `world_create` supports `bonusChest` and `generateStructures`
- `world_join` accepts `id` or `name`, but `id` is the stable choice
- `world_join` returns `world_name_ambiguous` if multiple saves share the same visible name
