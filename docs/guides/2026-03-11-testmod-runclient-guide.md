# TestMod RunClient Guide

Date: 2026-03-11 17:20 CST
Updated: 2026-03-12 08:10 CST

## Purpose

- run real Minecraft integration tests from a standalone Gradle project
- keep MCP implementation in the main repository
- make `TestMod` the default real-game `runClient` entrypoint

## Workflow

Project layout:

- main repo root:
  - `Server`
  - `Mod`
  - `TestMod`
- `TestMod` is a separate Gradle build
- `TestMod/settings.gradle` uses `includeBuild("..")` to consume this repository as an included build

Primary startup flow:

```powershell
cd TestMod
.\gradlew.bat runClient --no-daemon
```

What that does:

- starts the standalone `TestMod` NeoForge client
- loads the current `mod_dev_mcp` code from the included build
- starts the game MCP endpoint inside the Minecraft process

Current wiring:

- current included MCP runtime dependency coordinate:
  - `com.example.examplemod:mod_dev_mcp:0.1`
- default game MCP endpoint:
  - `127.0.0.1:47653`

Optional MCP bridge generation:

```powershell
.\gradlew.bat :Mod:createGameMcpBridgeLaunchScript --no-daemon
```

This writes:

- `..\Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat`
- `..\Mod\build\moddevmcp\game-mcp\game-mcp-bridge-java.args`

Quick verification:

```powershell
cd TestMod
.\gradlew.bat tasks --all --no-daemon
.\gradlew.bat compileJava --no-daemon
```

Expected:

- `runClient` appears in task output
- `compileJava` succeeds

## Notes

- `TestMod` is the current primary real-game validation path
- start game first, then connect the MCP client
- a typical MCP config command path is `<repo>\Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat`
- if Gradle dependency downloads fail, classify the failure separately as repository/TLS/environment issue
