# UI Intent Actions Design

Date: 2026-03-12 09:22 CST

## Goal

Replace the underdefined `moddev.ui_open` semantic tool with an explicit, test-oriented intent tool that does not pretend to generically "open" arbitrary UI.

## Problem

Current `moddev.ui_open` is misleading:

- it does not open a real Minecraft screen
- it ignores the caller's requested target or intent
- in `VanillaScreenUiDriver` and `FallbackRegionUiDriver` it only flips internal session state from closed to open
- the resulting focus is inferred later from resolver defaults, not from a concrete user-visible action

That means an MCP agent cannot safely treat `ui_open` as "open inventory", "open chat", or "open pause menu". The tool name suggests a real side effect that the runtime does not actually guarantee.

## Playwright Comparison

Playwright does not expose a generic `open` semantic for arbitrary page elements.

Instead it uses explicit primitives:

- navigate with `goto`
- create a page with `newPage`
- click a locator
- press a key
- wait for a popup, dialog, or URL change

For this MCP, the equivalent principle is:

- keep concrete same-screen actions as `click`, `hover`, `pressKey`, `typeText`
- represent cross-screen or high-level UI entry as explicit intents, not a vague `open`

## Options

### Option 1: Keep `ui_open` and tighten its docs

Pros:

- smallest code change

Cons:

- keeps a misleading public API
- agents will still over-trust the tool name
- does not align with Playwright-style explicit action design

### Option 2: Keep `ui_open` but require `mode` or `kind`

Pros:

- preserves name compatibility

Cons:

- still centers the wrong abstraction
- `open` remains overloaded between session restore and real game interaction

### Option 3: Remove `ui_open`, add explicit intent tool

Pros:

- public API becomes honest
- intent dispatch can be implemented per driver/runtime
- agent prompts become simpler: use ref actions for same-screen work, use intent calls for known high-level entry points

Cons:

- requires test and doc updates
- breaks old callers of `ui_open`

## Decision

Use Option 3.

Deprecate and remove `moddev.ui_open`, and introduce:

- `moddev.ui_run_intent`

This tool is explicitly high-level and intentionally small. It is not a generic replacement for `ui_action`.

## Public API

### `moddev.ui_run_intent`

Input:

- `intent`: required string
- `waitCondition`: optional string
- `waitTarget`: optional selector
- `waitTimeoutMs`: optional integer
- `waitPollIntervalMs`: optional integer
- `waitStableForMs`: optional integer

First supported intents:

- `inventory`
- `chat`
- `pause_menu`

Possible later additions:

- `recipe_book`
- `advancements`
- `stats`

Output shape should match the current semantic-action family where practical:

- `driverId`
- `action`
- `intent`
- `performed`
- `preSnapshotRef`
- `postSnapshotRef`
- `postActionSnapshot`
- optional `wait`

## Behavioral Rules

### Same-screen work

Do not use `ui_run_intent`.

Use:

- `moddev.ui_click_ref`
- `moddev.ui_hover_ref`
- `moddev.ui_press_key`
- `moddev.ui_type_text`
- `moddev.ui_switch`
- `moddev.ui_batch`

### High-level entry actions

Use `ui_run_intent` only when the action is truly an app-level entry intent that the runtime understands.

Examples:

- open player inventory
- open chat
- open pause menu

### Unknown intents

Return a structured failure:

- `unsupported_intent`

### Runtime not ready

Return existing readiness failures:

- `runtime_unavailable`
- `screen_unavailable`

## Architecture

`UiToolProvider` should keep owning the MCP surface, but the intent execution path should be explicit and separable from generic `ui_action`.

Recommended split:

- `UiToolProvider` parses `ui_run_intent`
- runtime resolves current `UiContext`
- selected driver executes `intent(...)`, or the provider delegates to a small intent executor abstraction

For the first pass, keep it minimal:

- add `ui_run_intent` in `UiToolProvider`
- implement supported intents for the vanilla runtime using existing input bridges
- keep `ui_switch` and `ui_close`
- remove `ui_open`

## Driver Expectations

### Vanilla runtime

Map intents to real player-facing triggers:

- `inventory` -> inventory key / equivalent runtime action
- `chat` -> chat key / equivalent runtime action
- `pause_menu` -> escape key / equivalent runtime action

These should result in actual screen changes or visible UI state changes, not only internal session toggles.

### Fallback driver

Do not fake screen opening.

Return:

- `unsupported_intent`

That keeps testing behavior honest.

## Documentation Impact

Update:

- `README.md`
- `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- historical design references that still list `ui_open` as a meaningful tool

The guidance should become:

- for same-screen actions, use ref actions or `ui_batch`
- for known high-level entry operations, use `ui_run_intent`

## Testing Strategy

### Unit and tool tests

- `ui_open` no longer registers
- `ui_run_intent` registers with the intended schema
- unsupported intent returns `unsupported_intent`
- fallback driver does not fake a successful open
- `ui_close` behavior remains intact
- `ui_switch` behavior remains intact

### Runtime tests

On live client:

- `ui_run_intent(intent = "pause_menu")` should reach the pause screen
- `ui_run_intent(intent = "chat")` should open chat when allowed
- `ui_run_intent(intent = "inventory")` should open inventory in a player context where that action is valid

## Acceptance Criteria

- `moddev.ui_open` is removed from active API surface
- `moddev.ui_run_intent` is explicit about supported high-level actions
- fallback paths no longer claim a fake successful open
- same-screen automation continues to rely on ref actions and batching
