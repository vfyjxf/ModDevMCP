# UI Driver Playwright-Debug API Implementation

Date: 2026-03-12 22:16 CST

## Status

Implemented and test-verified at the module level.

This plan strengthened the runtime-facing `UiDriver` contract and exposed a concise agent-facing debug workflow without removing the older low-level session/ref tools.

## Delivered Runtime API

- Added reusable low-level request/result models:
  - `UiTargetReference`
  - `UiLocator`
  - `UiResolveRequest`
  - `UiResolveResult`
  - `UiActionabilityResult`
  - `UiWaitRequest`
  - `UiWaitResult`
  - `UiInspectResult`
- Added `DefaultUiDriverSupport`.
- Wired `UiDriver` default methods for:
  - `resolve`
  - `checkActionability`
  - `waitFor`
  - `runIntent`
  - `inspect`

## Delivered Tooling

- Added concise high-level tools:
  - `moddev.ui_inspect`
  - `moddev.ui_act`
  - `moddev.ui_wait`
  - `moddev.ui_screenshot`
  - `moddev.ui_trace_recent`
- Kept lower-level fallback tools available:
  - `moddev.ui_session_open`
  - `moddev.ui_session_refresh`
  - `moddev.ui_click_ref`
  - `moddev.ui_hover_ref`
  - `moddev.ui_wait_for`
  - `moddev.ui_batch`
  - `moddev.ui_trace_get`

## Key Behavior Changes

- Locator/ref/point resolution is now modeled consistently through `UiTargetReference`.
- Actionability failures now return stable machine-facing codes such as `target_disabled` instead of tool-specific text.
- High-level inspect output is intentionally concise and omits raw snapshot noise by default.
- High-level screenshot output now returns a concise payload in live-screen mode:
  - `driverId`
  - `mode`
  - `resolvedTarget`
  - `snapshotRef`
  - `imageRef`
  - `imagePath`
  - `imageResourceUri`
  - `imageMeta`
  - `needsReinspect`
- Screenshot/capture still fail honestly when no real offscreen or framebuffer provider is available.
- High-level live-screen screenshot no longer requires every driver to override `capture`; real capture providers are enough.

## Recommended Agent Flow

1. `moddev.ui_get_live_screen`
2. `moddev.ui_run_intent` when a top-level screen must be opened
3. `moddev.ui_inspect`
4. `moddev.ui_act`
5. `moddev.ui_wait`
6. `moddev.ui_screenshot`
7. `moddev.ui_trace_recent`

Use session/ref tools only when several same-screen steps benefit from stable refs and trace history.

## Verification

- `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
  - Result: `BUILD SUCCESSFUL`
- `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --tests "*UiAutomationTraceTest" --tests "*UiAutomationBatchTest" --tests "*UiAutomationErrorHandlingTest" --tests "*DefaultUiDriverSupportTest" --tests "*VanillaScreenUiDriverTest" --tests "*UiDriverModelRecordsTest" --no-daemon`
  - Result: `BUILD SUCCESSFUL`

## Remaining Gap

- No fresh real-game `runClient` verification has been recorded yet for this specific high-level flow.
