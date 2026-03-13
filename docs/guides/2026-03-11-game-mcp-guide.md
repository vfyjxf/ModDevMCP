# Game MCP Guide

Date: 2026-03-11 17:20 CST
Updated: 2026-03-13 17:08 CST

## Purpose

- describe the current primary architecture
- show how agents reach the game through the host
- keep this legacy filename, but the content now documents the host-first flow

## Architecture

Primary runtime shape:

- `Server` hosts MCP and the host runtime listener
- `Mod` starts a reconnecting runtime client inside Minecraft
- agents connect only to `ModDevMcpStdioMain`
- runtime tools are exposed dynamically after the game connects
- `moddev.status` remains callable even with no game connected

## Startup Flow

Start the host MCP host:

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

## Verification

Recommended first calls after MCP connects:

1. `moddev.status`
2. `moddev.ui_get_live_screen`

Continue only after status reports `gameConnected=true`.

## Readiness Rule

Use this order:

1. start the host
2. start the game
3. call `moddev.status`
4. only then use snapshot/capture/input/inventory tools

If connection or the first tool call fails, treat the game as not ready.


