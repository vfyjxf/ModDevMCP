# TestMod RunClient Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-13 20:26 CST

## Purpose

- run a standalone NeoForge client against the current host-first ModDevMCP architecture
- use `TestMod` as the real `runClient` validation path

## Start the Host

From the repository root:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Server:runStdioMcp --no-daemon
```

## Start the Client

From `TestMod`:

```powershell
cd TestMod
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat runClient --no-daemon
```

Generate generic MCP client files:

```powershell
.\gradlew.bat createMcpClientFiles --no-daemon
```

## What This Does

- starts the standalone Minecraft client
- loads `mod_dev_mcp`
- initializes runtime providers in the game process
- connects the game runtime to the host on `127.0.0.1:47653`

## MCP Readiness

Recommended first checks from the agent side:

1. call `moddev.status`
2. verify `gameConnected=true`
3. call `moddev.ui_get_live_screen`

A typical MCP config should launch `dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain`, not a bridge script.


