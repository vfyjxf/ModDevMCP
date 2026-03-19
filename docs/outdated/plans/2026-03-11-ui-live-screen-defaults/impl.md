# UI Live Screen Defaults Implementation

Date: 2026-03-11 17:55 CST

## Status

Implementation completed.

## Changes

- updated `UiToolProvider.uiContext(...)` so `screenClass` defaults to the current `ClientScreenProbe` value only when the caller omits it
- kept explicit `screenClass` arguments authoritative and added a regression test to ensure explicit paths do not touch `LiveClientScreenProbe`
- kept `modId` behavior simple:
  - explicit `modId` wins
  - omitted `modId` still defaults to `minecraft`
- added focused tests for:
  - `ui_snapshot` without `screenClass`
  - `ui_capture` without `screenClass`
  - `ui_action` without `screenClass`
  - explicit `screenClass` should not read live screen

## Verification

Red step:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*UiToolInvocationTest.uiSnapshotUsesLiveScreenWhenScreenClassIsOmitted" --tests "*UiToolInvocationTest.uiCaptureUsesLiveScreenWhenScreenClassIsOmitted" --tests "*UiToolInvocationTest.uiActionUsesLiveScreenWhenScreenClassIsOmitted" --no-daemon
```

Real result:

- failed as expected before implementation
- failure shape:
  - omitted `screenClass` did not resolve to live screen

Focused green step:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*UiToolInvocationTest.uiSnapshotUsesLiveScreenWhenScreenClassIsOmitted" --tests "*UiToolInvocationTest.uiCaptureUsesLiveScreenWhenScreenClassIsOmitted" --tests "*UiToolInvocationTest.uiActionUsesLiveScreenWhenScreenClassIsOmitted" --tests "*UiToolInvocationTest.uiSnapshotDoesNotReadLiveScreenWhenScreenClassIsExplicit" --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

Shared test suite verification:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

## Real Runtime Verification

Environment cleanup before runtime verification:

- identified and stopped old stable server listener on port `47653`
- identified and stopped old Java client process that was holding `TestMod` runtime artifacts open

Real client start:

```powershell
cd TestMod
.\gradlew.bat runClient --no-daemon
```

Real result:

- launched successfully in a fresh process
- game MCP later opened `127.0.0.1:47653`

Bridge smoke call:

```powershell
@'
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05","capabilities":{},"clientInfo":{"name":"codex-smoke","version":"0.0.0"}}}
{"jsonrpc":"2.0","method":"notifications/initialized"}
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"moddev.ui_get_live_screen","arguments":{}}}
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"moddev.ui_capture","arguments":{"source":"auto"}}}
'@ | & "<repo>\Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat"
```

Real result:

- `moddev.ui_get_live_screen` succeeded
- returned:
  - `screenClass = net.minecraft.client.gui.screens.TitleScreen`
  - `driverId = vanilla-screen`
  - `active = true`
- `moddev.ui_capture` also succeeded without explicitly passing `screenClass`
- returned:
  - `driverId = vanilla-screen`
  - `imageMeta.source = offscreen`
  - `imageMeta.providerId = vanilla-offscreen`
  - `imagePath = <repo>\TestMod\run\build\moddevmcp\captures\capture-1.png`

Captured image check:

- file size: `506610` bytes
- visual check: valid Minecraft title screen screenshot
