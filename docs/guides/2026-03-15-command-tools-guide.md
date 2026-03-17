# Command Tools Guide

Date: 2026-03-15

## Purpose

Describe the command MCP tools used to discover and execute Minecraft commands inside the runtime selected by the normal gateway routing.

## Tools

- `moddev.command_list`
- `moddev.command_suggest`
- `moddev.command_execute`

## Routing Rule

- command tools follow the normal runtime routing already used by other dynamic tools
- when both `client` and `server` runtimes are connected, pass `targetSide=client|server` to select which runtime receives the tool call
- `targetSide=client` routes to the NeoForge client command runtime
- `targetSide=server` routes to the server command runtime

## Recommended Flow

1. call `moddev.status`
2. confirm `gameConnected=true`
3. choose `targetSide`
4. call `moddev.command_list` with a small `query`
5. call `moddev.command_suggest` when refining an argument-heavy command
6. call `moddev.command_execute`

## Minimal Examples

List matching server commands:

```json
{
  "name": "moddev.command_list",
  "arguments": {
    "targetSide": "server",
    "query": "time"
  }
}
```

Get suggestions for a client command:

```json
{
  "name": "moddev.command_suggest",
  "arguments": {
    "targetSide": "client",
    "input": "/clientconfig "
  }
}
```

Execute a server command:

```json
{
  "name": "moddev.command_execute",
  "arguments": {
    "targetSide": "server",
    "command": "/say hello from MCP"
  }
}
```

## Practical Notes

- `command_execute` normalizes a leading `/`
- `messages` contains bounded feedback lines, not an unbounded log dump
- parse or dispatch failures are returned as structured `errorCode` / `errorDetail`
