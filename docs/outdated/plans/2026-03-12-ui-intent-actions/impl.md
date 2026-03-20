# UI Intent Actions Implementation

Date: 2026-03-12 10:40 CST

## Status

Partially completed.

The `ui_open` surface has been removed from the active UI tool flow and replaced by `moddev.ui_run_intent` for explicit high-level entry actions. The runtime path is now honest:

- `ui_run_intent` delegates into the input controller chain
- supported vanilla intents map to real key-triggered input
- unsupported intents return `unsupported_intent`
- same-screen interaction remains on ref/key/text/switch tools

## Implementation Details

- Added `UiIntentKeyResolver` so `MinecraftInputController` can resolve intent keys without hard-wiring tests to live Minecraft state.
- Updated `MinecraftInputController` to:
  - accept injectable intent-key resolution for tests
  - use live key mapping for `inventory` and `chat`
  - keep `pause_menu` on `Esc`
- Added `InputActionDispatcher` to centralize input-controller dispatch and avoid duplicated branching between `InputToolProvider` and `UiToolProvider`.
- Changed input dispatch semantics so `unsupported_intent` is aggregated across controllers instead of stopping at the first controller.
- Updated `UiToolProvider.runIntentResult(...)` so it:
  - no longer special-cases `fallback-region`
  - no longer merges `controller` details into the result payload
  - returns `performed` from the accepted input controller
  - still records pre/post snapshots around the intent action

## Documentation Updates

- Active docs already point agents to `moddev.ui_run_intent` instead of `ui_open`:
  - `README.md`
  - `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- Historical design references touched in this round now describe `ui_open` as historical or replaced:
  - `docs/plans/2026-03-09-moddev-mcp-framework/design.md`
  - `docs/plans/2026-03-11-ui-live-screen-defaults/design.md`

## Verification

Focused behavior verification:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*MinecraftInputControllerTest" --tests "*InputToolProviderTest" --tests "*UiToolInvocationTest" --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

Focused provider/schema verification:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*MinecraftInputControllerTest" --tests "*InputToolProviderTest" --tests "*UiToolInvocationTest" --tests "*UiToolProviderTest" --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

## Remaining Work

- run live `runClient` verification for Task 5 and record real game-side transitions
- if that runtime pass reveals input-mapping edge cases, adjust the live resolver with real client evidence rather than more unit-only assumptions
