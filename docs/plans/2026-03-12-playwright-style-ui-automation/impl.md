# Playwright-Style UI Automation Implementation

Date: 2026-03-12 00:27 CST
Updated: 2026-03-12 22:16 CST

## Status

Implemented beyond the original thin session/ref path.

The stack now includes both:

- the older session/ref automation flow
- the newer concise high-level debug flow built on stronger `UiDriver` semantics

Real `runClient` verification and MCP agent end-to-end flow are still pending.

## Changes

- Strengthened `UiDriver` with reusable low-level models and default semantics:
  - `UiTargetReference`
  - `UiLocator`
  - `UiResolveRequest`
  - `UiResolveResult`
  - `UiActionabilityResult`
  - `UiWaitRequest`
  - `UiWaitResult`
  - `UiInspectResult`
- Added `DefaultUiDriverSupport` and wired default `resolve`, `checkActionability`, `waitFor`, `runIntent`, and `inspect`.
- Upgraded `VanillaScreenUiDriver` to provide better locator resolution and cleaner inspect output for vanilla widget screens.
- Added concise high-level debug tools:
  - `moddev.ui_inspect`
  - `moddev.ui_act`
  - `moddev.ui_wait`
  - `moddev.ui_screenshot`
  - `moddev.ui_trace_recent`
- Added in-process UI automation sessions in `Mod` with opaque per-session refs.
- Added thin automation tools:
  - `moddev.ui_session_open`
  - `moddev.ui_session_refresh`
  - `moddev.ui_click_ref`
  - `moddev.ui_hover_ref`
  - `moddev.ui_press_key`
  - `moddev.ui_type_text`
  - `moddev.ui_wait_for`
  - `moddev.ui_batch`
  - `moddev.ui_trace_get`
- Added session-owned trace entries with `stepIndex`, `type`, `elapsedMs`, `success`, and `errorCode`.
- Hardened failure mapping for:
  - `runtime_unavailable`
  - `screen_unavailable`
  - `session_not_found`
  - `session_stale`
  - `target_not_found`
  - `target_stale`
  - `batch_step_failed`
- Added refresh-on-demand so ref-based actions stay cheap on the same screen and refresh only when live state requires it.
- Fixed the same-`screenClass` stale path so ref actions refresh before misreporting stale refs as `target_not_found`.
- Normalized batch and trace `errorCode` to machine-stable codes and split human-readable text into `errorMessage`.
- Extended session trace recording so standalone session tools such as `ui_click_ref`, `ui_wait_for`, `ui_screenshot`, and `refresh` also appear in `moddev.ui_trace_get`.
- Changed screenshot behavior so high-level live-screen capture can succeed with real offscreen/framebuffer providers even when a driver does not override `capture`.
- Kept capture behavior strict: if no real capture provider is available, screenshot/capture paths return failure instead of placeholder output.
- Fixed nested vanilla widget extraction and press dispatch for tab-based screens:
  - Added `VanillaWidgetIntrospection`
  - `VanillaScreenUiDriver` now recursively discovers nested widget trees instead of only direct `screen.children()`
  - `VanillaWidgetPressSupport` now recursively matches nested widgets before invoking `onPress`
- This specifically unblocked `CreateWorldScreen` tab automation where `Game / World / More` were previously invisible to `moddev.ui_inspect` and unclickable through the high-level tool path.

## Verification

- Driver model tests:
  - Command: `.\gradlew.bat :Mod:test --tests '*UiDriverModelRecordsTest' --no-daemon`
  - Result: `BUILD SUCCESSFUL`
- Default support tests:
  - Command: `.\gradlew.bat :Mod:test --tests '*DefaultUiDriverSupportTest' --tests '*UiDriverModelRecordsTest' --no-daemon`
  - Result: `BUILD SUCCESSFUL`
- Vanilla driver tests:
  - Command: `.\gradlew.bat :Mod:test --tests '*VanillaScreenUiDriverTest' --tests '*DefaultUiDriverSupportTest' --tests '*UiDriverModelRecordsTest' --no-daemon`
  - Result: `BUILD SUCCESSFUL`
- High-level tool tests:
  - Command: `.\gradlew.bat :Mod:test --tests '*UiToolInvocationTest' --no-daemon`
  - Result: `BUILD SUCCESSFUL`
- Combined regression:
  - Command: `.\gradlew.bat :Mod:test --tests '*UiToolInvocationTest' --tests '*UiAutomationTraceTest' --tests '*UiAutomationBatchTest' --tests '*UiAutomationErrorHandlingTest' --tests '*DefaultUiDriverSupportTest' --tests '*VanillaScreenUiDriverTest' --tests '*VanillaWidgetIntrospectionTest' --tests '*UiDriverModelRecordsTest' --no-daemon`
  - Result: `BUILD SUCCESSFUL`
- Nested widget red-to-green verification:
  - Red command: `.\gradlew.bat :Mod:test --tests '*VanillaWidgetIntrospectionTest' --no-daemon`
  - Red result: failed at `compileTestJava` because `VanillaWidgetIntrospection` did not exist yet
  - Green command: `.\gradlew.bat :Mod:test --tests '*VanillaWidgetIntrospectionTest' --tests '*VanillaScreenUiDriverTest' --no-daemon`
  - Green result: `BUILD SUCCESSFUL`
- Real `runClient` verification from `TestMod`:
  - Command: `.\gradlew.bat runClient --no-daemon`
  - Result: game launched successfully, and log recorded `Started embedded game MCP on 127.0.0.1:47653`
  - External environment note: Mojang `minecraftservices` / `sessionserver` requests still timed out during startup; this was an external network timeout, not a project code failure
- Real live MCP verification on `CreateWorldScreen`:
  - `moddev.ui_act(locator={text:\"Create New World\"})` successfully opened `net.minecraft.client.gui.screens.worldselection.CreateWorldScreen`
  - `moddev.ui_inspect` on `CreateWorldScreen` now returned `Game`, `World`, and `More` with `widgetClass = net.minecraft.client.gui.components.TabButton`
  - `moddev.ui_act(locator={text:\"World\"})` successfully switched to the `World` tab
  - Real screenshot artifacts were written to:
    - `TestMod/run/build/moddevmcp/captures/capture-2.png`
    - `TestMod/run/build/moddevmcp/captures/capture-3.png`
    - `TestMod/run/build/moddevmcp/captures/capture-4.png`
- Task 4 focused tests:
  - Command: `.\gradlew.bat :Mod:test --tests '*UiAutomationErrorHandlingTest' --tests '*UiAutomationSessionManagerTest' --tests '*UiAutomationBatchTest' --tests '*UiToolInvocationTest' --tests '*UiAutomationTraceTest' --no-daemon --rerun-tasks`
  - Result: `BUILD SUCCESSFUL in 48s`
- Post-review red-to-green focused tests:
  - Red command: `.\gradlew.bat :Mod:test --tests '*UiAutomationBatchTest' --tests '*UiAutomationTraceTest' --tests '*UiToolInvocationTest' --no-daemon`
  - Red result: failed with 4 failing tests for same-class stale refresh, batch/trace errorCode normalization, and standalone session trace coverage
  - Green command: `.\gradlew.bat :Mod:test --tests '*UiAutomationBatchTest' --tests '*UiAutomationTraceTest' --tests '*UiToolInvocationTest' --no-daemon`
  - Green result: `BUILD SUCCESSFUL in 40s`
- Environment note:
  - This focused run did not hit Gradle TLS, repository, or dependency download failures.

## Current Recommended Workflow

1. `moddev.ui_get_live_screen`
2. `moddev.ui_run_intent` when a top-level screen must be opened
3. `moddev.ui_inspect`
4. `moddev.ui_act`
5. `moddev.ui_wait`
6. `moddev.ui_screenshot`
7. `moddev.ui_trace_recent`

Use the session/ref tools only when a longer same-screen flow benefits from persistent refs.

## Current Gaps

- `CreateWorldScreen` tab visibility and clickability are now real-game verified, but the longer end-to-end world-creation scenario is still unfinished.
- Some vanilla screens still expose duplicate text-derived target ids, such as the `StringWidget` + `EditBox` pair for `World Name`; this does not block tab automation, but it can still cause ambiguity for text-only locators.
- README and guide now match the recommended flow, but the full record for `create world -> configure world -> enter world -> inventory interaction` still needs to be completed.
