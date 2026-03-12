# Game MCP Implementation

Date: 2026-03-11 17:05 CST

## Status

Primary-path naming cleanup completed. Real runClient validation is still pending.

## Changes

- renamed the primary game-hosted bootstrap classes to:
  - `GameHostedMcpConfig` -> `GameMcpConfig`
  - `GameHostedMcpRuntime` -> `GameMcpRuntime`
  - `GameHostedMcpBridgeMain` -> `GameMcpBridgeMain`
- renamed the primary Mod Gradle bridge tasks to:
  - `runGameMcpBridge`
  - `writeGameMcpBridgeClasspath`
  - `createGameMcpBridgeLaunchScript`
- renamed generated bridge artifacts from `game-hosted-mcp/*` to `game-mcp/*`
- updated README and current guides to use `game MCP` / `游戏内 MCP` as the primary term
- renamed current primary design/plan/checklist/impl docs from `game-hosted-mcp-*` to `game-mcp-*`

## Verification

Focused verification command:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*GameMcpRuntimeTest" --tests "*GameMcpBridgeMainTest" --tests "*EmbeddedModDevMcpStdioMainTest" :Server:test --tests "*ModDevMcpStdioMainTest" --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`
- this verifies the renamed bootstrap classes, renamed Gradle bridge tasks, README references, and guide references are consistent
- this does not verify real `TestMod:runClient` startup yet

## Real Runtime Verification

Bridge generation command:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:createGameMcpBridgeLaunchScript --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`
- generated:
  - `Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat`
  - `Mod\build\moddevmcp\game-mcp\game-mcp-bridge-java.args`

Earlier `TestMod:runClient` attempt:

```powershell
$env:GRADLE_USER_HOME='..\.gradle-user'; .\gradlew.bat runClient --no-daemon
```

Real result:

- failed before launching a new client process
- failure point:
  - `AccessDeniedException: ... neoforge-21.1.219.jar.tmp -> ... neoforge-21.1.219.jar`
  - lock owner:
    - `30180 C:\Program Files\Zulu\zulu-21\bin\java.exe [Minecraft NeoForge* 1.21.1]`
- classification:
  - not a ModDevMCP code failure
  - this machine already had a running NeoForge client holding the TestMod runtime artifact open

Runtime connectivity checks against the already running game process:

```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 47653
```

Real result:

- `TcpTestSucceeded = True`

MCP bridge smoke call:

```powershell
@'
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05","capabilities":{},"clientInfo":{"name":"codex-smoke","version":"0.0.0"}}}
{"jsonrpc":"2.0","method":"notifications/initialized"}
{"jsonrpc":"2.0","id":2,"method":"tools/list"}
'@ | & "<repo>\Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat"
```

Real result:

- initialize succeeded
- `tools/list` returned `moddev.ui_get_live_screen`
- `tools/list` returned `moddev.ui_capture`

Live screen call:

```powershell
@'
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05","capabilities":{},"clientInfo":{"name":"codex-smoke","version":"0.0.0"}}}
{"jsonrpc":"2.0","method":"notifications/initialized"}
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"moddev.ui_get_live_screen","arguments":{}}}
'@ | & "<repo>\Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat"
```

Real result:

- success
- returned:
  - `screenClass = net.minecraft.client.gui.screens.TitleScreen`
  - `driverId = vanilla-screen`
  - `active = true`
  - `framebufferWidth = 854`
  - `framebufferHeight = 480`

Capture calls:

- first call without `screenClass` failed as designed for the current API contract:
  - `capture_unavailable: no real UI capture provider matched source=auto, driver=fallback-region, screenClass=custom.UnknownScreen`
- second call with explicit `screenClass = net.minecraft.client.gui.screens.TitleScreen` succeeded

Successful capture result:

- `imageRef = capture-2`
- `imageMeta.source = offscreen`
- `imageMeta.providerId = vanilla-offscreen`
- `imagePath = <repo>\TestMod\run\build\moddevmcp\captures\capture-2.png`

Captured image file:

- path: `<repo>\TestMod\run\build\moddevmcp\captures\capture-2.png`
- file size: `526654` bytes
- visual check: valid title-screen screenshot, not an empty image and not a placeholder fallback

## Follow-up Runtime Verification After Cleanup

Additional root-cause cleanup:

- confirmed `127.0.0.1:47653` was later occupied by an old stable server process:
  - `PID 61232`
  - command line: `@<repo>\.moddevmcp\stable-server\stable-mcp-java.args`
- stopped that old stable server process
- stopped the remaining old Java client process so `TestMod` could relaunch cleanly

Fresh real client launch:

```powershell
cd TestMod
.\gradlew.bat runClient --no-daemon
```

Real result:

- client processes started cleanly
- after startup, `127.0.0.1:47653` was listening again from the new runtime

Fresh MCP smoke result:

- `initialize` succeeded
- `moddev.ui_get_live_screen` succeeded
- `moddev.ui_capture(source=auto)` succeeded on the new run

Fresh runtime values:

- `screenClass = net.minecraft.client.gui.screens.TitleScreen`
- `driverId = vanilla-screen`
- `imageMeta.source = offscreen`
- `imageMeta.providerId = vanilla-offscreen`
- `imagePath = <repo>\TestMod\run\build\moddevmcp\captures\capture-1.png`

Fresh captured image:

- path: `<repo>\TestMod\run\build\moddevmcp\captures\capture-1.png`
- file size: `506610` bytes
- visual check: valid title-screen screenshot from the fresh run
