# Local World Operations Guide

Date: 2026-03-15
Updated: 2026-03-20

## Purpose

Use `world.list`, `world.create`, and `world.join` to manage local singleplayer saves.

## Operations

- `world.list`
- `world.create`
- `world.join`

## Notes

- local world operations are client-side even if an integrated server is present
- `worldId` is the save folder id, not just a display label

## Examples

List local worlds:

```json
{
  "operationId": "world.list",
  "targetSide": "client",
  "input": {}
}
```

Create a new world:

```json
{
  "operationId": "world.create",
  "targetSide": "client",
  "input": {
    "name": "Test World",
    "gameMode": "survival",
    "allowCheats": false
  }
}
```

Join an existing world:

```json
{
  "operationId": "world.join",
  "targetSide": "client",
  "input": {
    "id": "MyWorld"
  }
}
```
