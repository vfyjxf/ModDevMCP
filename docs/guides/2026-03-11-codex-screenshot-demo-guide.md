# 2026-03-11 Codex Screenshot Demo Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-13 17:08 CST

## Goal

- connect a Codex-style MCP client to the host entrypoint
- verify game readiness with `moddev.status`
- capture and inspect a real screenshot after the game connects

## Startup Order

1. start the host MCP host
2. start `TestMod` `runClient`
3. wait for the game to load
4. start Codex or let Codex launch the MCP entry

## Repository Commands

Host:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Server:runStdioMcp --no-daemon
```

Game:

```powershell
cd TestMod
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat runClient --no-daemon
```

## Codex Rule

First calls:

1. `moddev.status`
2. `moddev.ui_get_live_screen`
3. `moddev.ui_capture`

If status reports `gameConnected=false`, stop and wait for the game.


