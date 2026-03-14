# 2026-03-11 TestMod RunClient Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-15 00:05 CST

## Purpose

- run a standalone NeoForge client for real validation
- keep `TestMod` as the reference consumer project
- keep MCP client install files and the game run flow aligned

## Consumer Shape

- `TestMod` is wired as a normal consumer project
- it does not need an explicit `modDevMcp {}` block for the default client flow
- `runClient` depends on `createMcpClientFiles`
- your MCP client starts the generated host entry from the installed config

## Start the Client

From `TestMod`:

```powershell
cd TestMod
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat runClient --no-daemon
```

Generate client files manually when needed:

```powershell
.\gradlew.bat createMcpClientFiles --no-daemon
```

`runClient` also depends on client file generation, so the MCP install files stay in sync.

## What Users Should Expect

- the Minecraft client starts
- the mod loads with the game
- the game connects back to the host automatically after the MCP client starts the generated host entry
- the generated MCP client files stay aligned with the current build

## Agent Readiness Check

1. call `moddev.status`
2. verify `gameConnected=true`
3. call `moddev.ui_get_live_screen`

Only continue after all three steps complete successfully.
