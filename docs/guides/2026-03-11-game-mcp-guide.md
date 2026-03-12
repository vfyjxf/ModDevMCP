# Game MCP Guide

Date: 2026-03-11 17:20 CST
Updated: 2026-03-12 08:10 CST

## Purpose

- describe the current primary architecture
- show how to connect MCP clients after the game is already running

## Architecture

Primary runtime shape:

- Minecraft process hosts MCP directly
- default endpoint is `127.0.0.1:47653`
- embedded MCP host/bootstrap code lives in `Server`
- `Mod` provides runtime/tool implementations and starts that embedded host from the client entrypoint
- MCP clients connect only after the game is already up
- there is no stable server in the primary path

## Startup Flow

Start game:

```powershell
cd TestMod
.\gradlew.bat runClient --no-daemon
```

Generate the connect-only bridge if needed:

```powershell
.\gradlew.bat :Mod:createGameMcpBridgeLaunchScript --no-daemon
```

Bridge outputs:

- `Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat`
- `Mod\build\moddevmcp\game-mcp\game-mcp-bridge-java.args`

Example MCP config:

```toml
[mcp_servers.moddevmcp]
command = '<repo>\\Mod\\build\\moddevmcp\\game-mcp\\run-game-mcp-bridge.bat'
```

## Verification

Recommended first call after MCP connects:

- `moddev.ui_get_live_screen`

Continue only if it succeeds.

## Readiness Rule

Use this order:

1. start the game
2. connect the MCP client
3. call `moddev.ui_get_live_screen`
4. only then use snapshot/capture/input/inventory tools

If connection or the first tool call fails, treat the game as not ready.
