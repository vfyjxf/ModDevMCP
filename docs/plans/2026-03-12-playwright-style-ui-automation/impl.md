# Playwright-Style UI Automation Implementation

Date: 2026-03-12 00:27 CST

## Status

Implemented through Task 4, including post-review fixes, and test-verified in `:Mod:test`.

Real `runClient` verification and MCP agent end-to-end flow are still pending.

## Changes

- Added in-process UI automation sessions in `Mod` with opaque per-session refs.
- Added thin automation tools:
  - `moddev.ui_session_open`
  - `moddev.ui_session_refresh`
  - `moddev.ui_click_ref`
  - `moddev.ui_hover_ref`
  - `moddev.ui_press_key`
  - `moddev.ui_type_text`
  - `moddev.ui_wait_for`
  - `moddev.ui_screenshot`
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
- Kept capture behavior strict: if no real capture provider is available, screenshot/capture paths return failure instead of placeholder output.

## Verification

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

## Current Gaps

- No fresh real-game runtime verification has been recorded yet for the new thin automation tools.
- No real screenshot artifact or real trace excerpt has been recorded yet for `ui_session_open -> ui_batch/ui_screenshot -> ui_trace_get`.
- README and guide now describe the intended agent path, but the final real-flow record still needs a live game session.
