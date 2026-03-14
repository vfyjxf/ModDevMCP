# TestMod RunClient Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-14 14:40 CST

## Purpose

- run a standalone NeoForge client against the current gateway/backend architecture
- use `TestMod` as the real `runClient` validation path for the host-first flow

## Start the Gateway

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

`runClient` also depends on `createMcpClientFiles`, so the generated MCP files stay in sync without an extra manual step.

## What This Does

- starts the standalone Minecraft client
- loads `mod_dev_mcp`
- initializes runtime providers in the game process
- connects the game runtime to the backend on `127.0.0.1:47653`
- leaves MCP ownership to the standalone host gateway process

## Generated Files

The files to hand to agent users live under:

- `build/moddevmcp/mcp-clients/clients/codex.toml`
- `build/moddevmcp/mcp-clients/clients/mcp-servers.json`
- `build/moddevmcp/mcp-clients/clients/INSTALL.md`

## MCP Readiness

Recommended first checks from the agent side:

1. call `moddev.status`
2. verify `gameConnected=true`
3. call `moddev.ui_get_live_screen`

A typical MCP install should reuse the generated files under `build/moddevmcp/mcp-clients/clients/`, not a hand-written `java -cp ...` command.
