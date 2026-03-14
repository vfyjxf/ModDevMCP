# Game MCP Guide

Date: 2026-03-11 17:20 CST
Updated: 2026-03-14 14:40 CST

## Purpose

- describe the current gateway/backend/game architecture
- show how agents reach the game through the standalone server
- keep this legacy filename, but document the current flow only

## Architecture

Primary runtime shape:

- `Server` starts the stdio gateway seen by MCP clients
- the gateway auto-starts the backend when needed
- the backend is the stable state center
- `Mod` starts a reconnecting runtime client inside Minecraft
- agents connect only to `:Server:runStdioMcp`
- runtime tools are exposed dynamically after the game connects
- `moddev.status` remains callable even with no game connected

## Startup Flow

Start the gateway:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Server:runStdioMcp --no-daemon
```

Start the game:

```powershell
cd TestMod
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat runClient --no-daemon
```

## Generated MCP Configs

Use the generated files from:

- `TestMod/build/moddevmcp/mcp-clients/clients/codex.toml`
- `TestMod/build/moddevmcp/mcp-clients/clients/mcp-servers.json`
- `TestMod/build/moddevmcp/mcp-clients/clients/INSTALL.md`

These generated configs already point at the gateway and include the backend bootstrap properties.

## Verification

Recommended first calls after MCP connects:

1. `moddev.status`
2. `moddev.ui_get_live_screen`

Continue only after status reports `gameConnected=true`.

## Readiness Rule

Use this order:

1. start the gateway
2. start the game
3. call `moddev.status`
4. only then use snapshot, capture, input, inventory, or UI tools

If connection or the first tool call fails, treat the game as not ready.
