# Title Singleplayer Flow Implementation

Date: 2026-03-11 19:50 CST

## Status

Implemented and runtime-verified.

## Changes

- Added `tools/runtime/game-mcp-title-singleplayer-flow.ps1`.
- The script now:
  - reads the live title screen
  - queries `button-singleplayer`
  - computes its GUI-space center
  - clicks it through `moddev.input_action`
  - polls `moddev.ui_get_live_screen` until `SelectWorldScreen`
  - captures the resulting screen and writes `step-log.json`
- The second capture now waits 500 ms after screen transition and explicitly uses `source = "framebuffer"` so world-selection widgets appear in the saved screenshot.

## Verification

- Real runtime command:
  - `powershell -NoProfile -ExecutionPolicy Bypass -File tools/runtime/game-mcp-title-singleplayer-flow.ps1`
- First runtime result:
  - flow completed and switched to `net.minecraft.client.gui.screens.worldselection.SelectWorldScreen`
  - output directory: `build/demo/title-singleplayer-flow/20260311-195711`
  - issue: second screenshot only showed the panorama/background even though the log already reported `SelectWorldScreen` targets
- Adjustment:
  - added a 500 ms post-transition wait
  - changed the second capture to explicit framebuffer capture
- Final runtime result:
  - flow completed successfully
  - output directory: `build/demo/title-singleplayer-flow/20260311-200526`
  - title screenshot: `build/demo/title-singleplayer-flow/20260311-200526/step-01-title-screen.png`
  - world-selection screenshot: `build/demo/title-singleplayer-flow/20260311-200526/step-02-select-world-screen.png`
  - step log: `build/demo/title-singleplayer-flow/20260311-200526/step-log.json`

## Runtime Observations

- The script queried exactly one title-screen target:
  - `targetId = button-singleplayer`
  - bounds: `x = 113`, `y = 92`, `width = 200`, `height = 20`
- The script clicked GUI coordinates:
  - `x = 213`
  - `y = 102`
- The world-selection screen was reached successfully:
  - `screenClass = net.minecraft.client.gui.screens.worldselection.SelectWorldScreen`
- The final step log on the world-selection screen includes real extracted targets such as:
  - `button-create-new-world`
  - `button-play-selected-world`
  - `button-back`
- The final framebuffer screenshot visibly shows:
  - `Select World`
  - the world list
  - `Create New World`
  - `Back`

## Next Gap

- The next practical flow is clicking `Create New World`, then driving the create-world configuration UI with the same per-step screenshot/log discipline.
