# Hover And Runtime Flow Implementation

Date: 2026-03-11 18:35 CST

## Status

Implemented and runtime-verified.

## Changes

- Added `move` and `hover` support to `moddev.input_action`.
- Added shared simulated pointer tracking via `UiPointerStateRegistry`.
- Wired `MinecraftInputController` to remember pointer coordinates for `click` / `move` / `hover`.
- Wired `UiToolProvider` to reuse tracked pointer coordinates when `mouseX` / `mouseY` are omitted.
- Added `tools/runtime/game-mcp-title-flow.ps1` for repeatable title-screen hover regression with per-step screenshots and `step-log.json`.

## Verification

- Focused tests:
  - Command: `.\gradlew.bat :Mod:test --tests "*MinecraftInputControllerTest" --tests "*UiToolInvocationTest" --no-daemon`
  - Result: `BUILD SUCCESSFUL`
- First runtime script run:
  - Command: `powershell -NoProfile -ExecutionPolicy Bypass -File tools/runtime/game-mcp-title-flow.ps1`
  - Result: failed in script parsing because local PowerShell `ConvertFrom-Json` did not support `-Depth`
  - Fix: removed `-Depth` from script JSON parsing
- Second runtime script run:
  - Result: `moddev.input_action` returned `unsupported input action: hover`
  - Root cause: connected game process was an older `runClient` instance started before the new hover classes were compiled
  - Fix: stopped the old `runClient` Java processes and relaunched `TestMod\gradlew runClient`
- Final runtime script run:
  - Command: `powershell -NoProfile -ExecutionPolicy Bypass -File tools/runtime/game-mcp-title-flow.ps1`
  - Result: completed successfully
  - Output directory: `build/demo/title-hover-flow/20260311-190826`
  - Pre-hover screenshot: `build/demo/title-hover-flow/20260311-190826/step-01-pre-hover.png`
  - Post-hover screenshot: `build/demo/title-hover-flow/20260311-190826/step-02-post-hover.png`
  - Step log: `build/demo/title-hover-flow/20260311-190826/step-log.json`

## Runtime Observations

- `ui_get_interaction_state` after hover returned `cursorX = 214` and `cursorY = 118`.
- Post-hover screenshot visibly highlighted the `Multiplayer` button, confirming the simulated GUI-space hover now reaches offscreen capture output.
- Later follow-up work added title-screen widget extraction, so `hoveredTarget` is now available for title buttons as well.

## Remaining Gap

- Title-screen capture and cursor tracking now work for simulated hover.
- Next practical step is scripted click flow across title screen and world-selection screens with per-step screenshots.
