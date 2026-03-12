# 2026-03-11 Simple Agent Install Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-12 08:10 CST

## Purpose

- give MCP clients one simple project-local command
- keep the runtime inside Minecraft
- avoid any separate server startup in the normal flow

See also:

- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-agent-prompt-templates.md`

## Workflow

Game-hosted integration contract:

- the Minecraft process is the real MCP host
- default endpoint is `127.0.0.1:47653`
- the external bridge only connects to that endpoint
- the bridge does not start the game

Setup:

1. Start Minecraft with ModDevMCP loaded.
2. Generate the connect-only bridge:

```powershell
.\gradlew.bat :Mod:createGameMcpBridgeLaunchScript --no-daemon
```

This writes:

- `Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat`
- `Mod\build\moddevmcp\game-mcp\game-mcp-bridge-java.args`

Minimal MCP client setup:

- configure the client to launch `run-game-mcp-bridge.bat`

Example:

```toml
[mcp_servers.moddevmcp]
command = '<repo>\\Mod\\build\\moddevmcp\\game-mcp\\run-game-mcp-bridge.bat'
```

## Verification

Agent rule:

1. Start the game first.
2. Start the MCP client second.
3. First call `moddev.ui_get_live_screen`.
4. If connection or that call fails, stop and ask the user to finish loading the game.
