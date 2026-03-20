# Command Operations Guide

Date: 2026-03-15
Updated: 2026-03-20

## Purpose

Describe the command operations used to discover and execute Minecraft commands inside the selected runtime.

## Operations

- `command.list`
- `command.suggest`
- `command.execute`

## Routing Rule

- command operations follow the normal `targetSide` routing
- when both `client` and `server` runtimes are connected, pass `targetSide=client|server`
- `targetSide=client` routes to NeoForge client commands
- `targetSide=server` routes to server commands

## Recommended Flow

1. `GET /api/v1/status`
2. confirm `gameReady=true`
3. choose `targetSide`
4. `command.list` with a small `query`
5. `command.suggest` when refining arguments
6. `command.execute`

## Minimal Examples

List matching server commands:

```json
{
  "operationId": "command.list",
  "targetSide": "server",
  "input": {
    "query": "time"
  }
}
```

Get suggestions for a client command:

```json
{
  "operationId": "command.suggest",
  "targetSide": "client",
  "input": {
    "input": "/clientconfig "
  }
}
```

Execute a server command:

```json
{
  "operationId": "command.execute",
  "targetSide": "server",
  "input": {
    "command": "/say hello from service"
  }
}
```

## Practical Notes

- `command.execute` normalizes a leading `/`
- `messages` contains bounded feedback lines
- parse or dispatch failures return structured `errorCode` / `errorDetail`
