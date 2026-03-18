# Local World Operations Guide

Date: 2026-03-15

Updated: 2026-03-18 14:50 CST

## Purpose

- explain the public HTTP operations for local singleplayer worlds
- show the correct request envelope and side rules
- document the returned `worldId` shape and common failure modes

## Public Operations

- `world.list`
- `world.create`
- `world.join`

These are public service operations sent through `POST /api/v1/requests`.

The internal runtime providers still use `moddev.world_list`, `moddev.world_create`, and `moddev.world_join`, but agents should target the public operation ids unless they are working inside the mod codebase itself.

## Scope

- local singleplayer worlds only
- client-side world flow only
- no multiplayer server support
- no delete, rename, import, or export support

## Required Flow

1. call `GET /api/v1/status`
2. confirm `serviceReady=true`
3. confirm `gameReady=true` before create or join work
4. call `world.list` when you need an existing save id
5. call `world.join` with `worldId` when entering an existing save
6. call `world.create` when you need a fresh local test world

## Target Side Rule

- `world.*` operations are client-only
- if the current session exposes exactly one eligible side, `targetSide` may be omitted
- sending `targetSide:"client"` is also valid and can make scripts clearer

## Request Examples

List local worlds:

```json
{
  "requestId": "world-list-1",
  "operationId": "world.list",
  "input": {}
}
```

Create and join a creative test world:

```json
{
  "requestId": "world-create-1",
  "operationId": "world.create",
  "targetSide": "client",
  "input": {
    "name": "MCP Test World",
    "gameMode": "creative",
    "allowCheats": true,
    "worldType": "default",
    "difficulty": "normal",
    "generateStructures": true
  }
}
```

Create and join a superflat build world:

```json
{
  "requestId": "world-create-flat-1",
  "operationId": "world.create",
  "targetSide": "client",
  "input": {
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
  "requestId": "world-join-1",
  "operationId": "world.join",
  "targetSide": "client",
  "input": {
    "id": "Flat Build World"
  }
}
```

## Response Shape

`world.list` returns:

- `worlds[].id`
- `worlds[].name`
- `worlds[].lastPlayed`
- `worlds[].gameMode`
- `worlds[].hardcore`
- `worlds[].cheatsKnown`

`world.create` returns:

- `worldId`
- `worldName`
- `created`
- `joined`

`world.join` returns:

- `worldId`
- `worldName`
- `joined`

## World Id Semantics

- `worldId` is the local save-folder id, not just the visible display name
- `world.list` returns the stable ids to reuse later
- `world.create` now returns the id of the world that is actually open after creation and join succeeds
- after a successful `world.create`, reuse the returned `worldId` for later `world.join` calls

## Practical Notes

- `world.create` defaults `joinAfterCreate=true`
- `world.create` currently requires `joinAfterCreate=true`
- supported `worldType`: `default|flat|large_biomes|amplified`
- supported `difficulty`: `peaceful|easy|normal|hard`
- `world.create` supports `bonusChest` and `generateStructures`
- `world.join` accepts `id` or `name`, but `id` is the stable choice
- `world.join` returns `world_name_ambiguous` if multiple saves share the same visible name

## Failure Model

- `world_not_found`: the requested existing save does not exist
- `world_name_ambiguous`: multiple saves match the requested visible name
- `world_create_failed`: create flow failed or the game did not enter the requested world
- `world_join_failed`: join flow failed or the game did not enter the requested world
- `world_action_unavailable`: client runtime or required APIs are unavailable
- `world_already_open`: the requested world is already open

`world.create` should not report `world_not_found` after the game has already entered the new world. If that happens, treat it as a bug in result reporting rather than a user input problem.
