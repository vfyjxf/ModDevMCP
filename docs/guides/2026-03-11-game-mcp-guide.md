# 2026-03-11 Game MCP Guide

Date: 2026-03-11 17:20 CST
Updated: 2026-03-15 00:05 CST

## Purpose

- explain the user-facing runtime flow
- show how the host, game, and agent fit together
- keep the operational rule simple

## Runtime Shape

- for a normal consumer setup, install the plugin and published mod dependency without adding a `modDevMcp {}` block
- generate and install MCP client config once with `createMcpClientFiles`
- install the generated MCP client config into your MCP client
- start your normal game run, such as `runClient`
- let the game connect to the host automatically
- connect through the generated MCP entry
- use `moddev.status` before any game-specific tool

## Startup Flow

Start the game from your project:

```powershell
.\gradlew.bat runClient --no-daemon
```

Then connect your MCP client with the generated ModDevMCP config. The MCP client starts the host entry automatically from that config.

## First Agent Calls

Recommended order:

1. `moddev.status`
2. `moddev.ui_get_live_screen`
3. `moddev.command_list` if you need command discovery on `client` or `server`
4. `moddev.world_list` if you need local singleplayer world discovery on the client runtime

Continue only if:

- `gameConnected=true`
- the live screen call succeeds

## Practical Rule

Do not use UI, input, inventory, or capture tools before readiness is confirmed.

If connection or the first tool call fails, treat the game as not ready.

## Command Tools

The gateway also exposes side-aware command tools:

- `moddev.command_list`
- `moddev.command_suggest`
- `moddev.command_execute`

Command tools use `commandSide` to choose client or server command context inside the runtime selected by the normal gateway routing.

## Local World Tools

The client runtime also exposes local-world tools:

- `moddev.world_list`
- `moddev.world_create`
- `moddev.world_join`

These tools operate on local singleplayer saves only and drive the real client into the selected world.

For a normal consumer setup, you do not need a `modDevMcp {}` block to use this runtime flow.
