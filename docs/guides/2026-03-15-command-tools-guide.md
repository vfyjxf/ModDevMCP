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
- `commandSide` selects which command dispatcher to use inside the chosen runtime
- use `commandSide=client` for NeoForge client commands
- use `commandSide=server` for vanilla / modded server commands
- when the chosen runtime is `client runtime`, `commandSide=server` targets the integrated server dispatcher if a singleplayer server is running
- do not pass `targetSide` for command tools; the gateway already uses that key for runtime routing

## Recommended Flow

1. call `moddev.status`
2. confirm `gameConnected=true`
3. call `moddev.command_list` with a small `query`
4. call `moddev.command_suggest` when refining an argument-heavy command
5. call `moddev.command_execute` only after choosing the command side explicitly

## Minimal Examples

List matching server commands:

```json
{
  "name": "moddev.command_list",
  "arguments": {
    "commandSide": "server",
    "query": "time"
  }
}
```

Get suggestions for a client command:

```json
{
  "name": "moddev.command_suggest",
  "arguments": {
    "commandSide": "client",
    "input": "/clientconfig "
  }
}
```

Execute a server command:

```json
{
  "name": "moddev.command_execute",
  "arguments": {
    "commandSide": "server",
    "command": "/say hello from MCP"
  }
}
```

## Practical Notes

- `command_execute` normalizes a leading `/`
- `messages` contains bounded feedback lines, not an unbounded log dump
- parse or dispatch failures are returned as structured `errorCode` / `errorDetail`
- `commandSide=server` is not limited to the dedicated `server runtime`; in singleplayer it can be served by the client-side integrated server
