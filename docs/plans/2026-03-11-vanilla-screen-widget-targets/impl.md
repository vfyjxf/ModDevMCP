# Vanilla Screen Widget Targets Implementation

Date: 2026-03-11 19:20 CST

## Status

Implemented and runtime-verified.

## Changes

- Added `Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/VanillaScreenUiDriverTest.java` to cover:
  - extracted title-screen button targets
  - pointer-derived hovered target
  - direct query and interaction state behavior
- Updated `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaScreenUiDriver.java`:
  - supports injected target extraction for tests
  - extracts live button widgets on real vanilla screens
  - derives `hoveredTargetId` from `UiContext.mouseX/Y` when no session/default override exists
  - keeps ordinary unit tests decoupled from hard client-class loading by using reflection in the live extractor

## Verification

- Red test:
  - Command: `.\gradlew.bat :Mod:test --tests "*VanillaScreenUiDriverTest" --no-daemon`
  - Initial result: failed because `VanillaScreenUiDriver` did not yet expose an extractor-aware constructor
- Green tests:
  - Command: `.\gradlew.bat :Mod:test --tests "*VanillaScreenUiDriverTest" --no-daemon`
  - Result: `BUILD SUCCESSFUL`
- Broader regression:
  - Command: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --tests "*VanillaScreenUiDriverTest" --no-daemon`
  - First result: failed with `NoClassDefFoundError` in `UiToolInvocationTest`
  - Root cause: new live extractor directly referenced Minecraft client classes during default driver construction
  - Fix: changed the live extractor to reflection-based access so non-game unit tests do not hard-load client classes
  - Final result: `BUILD SUCCESSFUL`
- Real runtime:
  - Command: `powershell -NoProfile -ExecutionPolicy Bypass -File tools/runtime/game-mcp-title-flow.ps1`
  - Result: completed successfully
  - Output directory: `build/demo/title-hover-flow/20260311-193932`

## Runtime Observations

- `capturedSnapshot.targets` on the title screen now includes real button targets such as:
  - `button-singleplayer`
  - `button-multiplayer`
  - `button-options`
  - `button-quit-game`
- After hover:
  - `interactionState.hoveredTarget.targetId = button-multiplayer`
  - `capturedSnapshot.targets[*].state.hovered` is `true` on `button-multiplayer`
  - screenshot still visually shows the `Multiplayer` button highlighted

## Next Gap

- Title-screen target extraction is now usable for scripted click flows.
- The next implementation step is to drive `Singleplayer` click, wait for the next screen, and keep per-step screenshots/logs.
