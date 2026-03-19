# Playwright-Style UI Automation Design

Date: 2026-03-11 23:00 CST

## Goal

Add a Playwright-style thin automation layer on top of the existing `moddev` UI and input tools so MCP agents can drive Minecraft GUI flows with fewer round trips, stable per-session refs, structured trace output, and optional batched actions.

## Context

The current runtime already has the low-level capabilities needed for real GUI automation:

- `moddev.ui_snapshot`
- `moddev.ui_query`
- `moddev.ui_action`
- `moddev.ui_wait`
- `moddev.ui_capture`
- `moddev.input_action`
- `moddev.ui_get_live_screen`

Those tools are functional, but agent usage is still too verbose and slow because a typical flow repeatedly performs:

1. full snapshot or query
2. target selection logic in the agent
3. one low-level action
4. extra wait
5. extra capture for inspection

This design keeps the existing low-level tools intact and adds a higher-level automation surface for agents.

## In Scope

- add a thin Playwright-style automation tool family
- add in-process UI automation sessions
- add stable per-session refs that resolve back to current targets
- add sequential batch execution with structured per-step results
- add structured trace recording for automation sessions
- keep screenshots explicit or failure-triggered, not mandatory on every step

## Out of Scope

- no new stable backend or startup supervisor layer
- no fallback screenshot generation when real capture is unavailable
- no cross-restart session persistence
- no rewrite of the existing `ui_*` low-level tools
- no inventory capability expansion in this step

## Recommended Approach

Use a thin wrapper layer on top of the current runtime:

- the existing `UiToolProvider` and `InputToolProvider` remain the capability source
- new automation session classes hold the latest snapshot, ref mapping, and trace
- new MCP tools expose short, agent-friendly operations
- batch execution internally composes the same snapshot/action/wait/capture primitives already used today

This keeps the architecture simple:

- `Server` remains transport and MCP protocol glue
- `Mod` remains the only place that knows the live game UI and input runtime
- automation state stays inside the game process and disappears when the game closes

## Public Tool Surface

Add these tools:

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

### Tool Roles

`moddev.ui_session_open`
- creates a short-lived automation session
- probes the live screen
- captures an initial snapshot
- returns `sessionId`, `screenClass`, `driverId`, and generated refs

`moddev.ui_session_refresh`
- refreshes the session from the current live screen
- rebuilds target refs from the latest snapshot
- returns whether the screen changed

`moddev.ui_click_ref` and `moddev.ui_hover_ref`
- resolve a session ref to the latest current target
- execute the action without requiring the agent to perform a new query
- optionally include a post-action wait

`moddev.ui_press_key` and `moddev.ui_type_text`
- wrap `moddev.input_action`
- keep keyboard actions inside the same automation session trace

`moddev.ui_wait_for`
- waits by ref, selector, or screen-level condition
- provides one consistent wait surface for the agent path

`moddev.ui_screenshot`
- captures the current screen or a selected ref target
- stores and returns the artifact path and metadata

`moddev.ui_batch`
- accepts a sequential list of automation steps
- executes them inside one session with one MCP call
- supports `stopOnError`
- records per-step results in trace

`moddev.ui_trace_get`
- returns structured trace entries for the session
- includes step result summaries, elapsed time, error code, and optional screenshot metadata

## Session Model

Sessions are in-memory runtime objects owned by the game process.

Each session stores:

- `sessionId`
- last known `screenId`
- last known `screenClass`
- last `UiSnapshot`
- current ref-to-target mapping
- ordered trace entries
- creation time and last refresh time

Sessions are intentionally short-lived and local:

- they are valid only for the current game process
- they are not written to disk
- they become stale when the target cannot be resolved against the current UI state

## Ref Model

Refs are stable only inside one session.

Recommended representation:

- opaque string ref returned to the caller
- internally mapped to:
  - driver id
  - originating screen id
  - target id
  - snapshot generation number

Resolution rule:

1. refresh or inspect the latest snapshot for the session
2. attempt to find the same target id on the current screen
3. if the screen changed or the target no longer exists, return `target_stale` or `target_not_found`

This keeps refs stable enough for multi-step GUI flows without pretending they survive game restarts or large UI transitions.

## Batch Execution

`moddev.ui_batch` executes a list of step objects in order.

Supported first-pass step kinds:

- `refresh`
- `clickRef`
- `hoverRef`
- `pressKey`
- `typeText`
- `waitFor`
- `screenshot`

Batch defaults:

- no screenshot after every step
- only refresh snapshot state when needed by the action or wait
- stop at first failure unless `stopOnError = false`

Batch result includes:

- session id
- final screen metadata
- per-step status list
- optional failure step index
- trace summary

## Trace Model

Each session accumulates ordered trace entries.

Each entry should include:

- step index
- step kind
- minimal argument summary
- start and end timestamps
- elapsed milliseconds
- screen class before and after
- resolved ref or target id, when applicable
- success or failure
- error code and message, when applicable
- screenshot path, only when explicitly captured or captured on failure

Trace is intended for machine use first and human inspection second.

## Error Handling

Use explicit structured errors instead of fallback behavior.

Required error codes:

- `runtime_unavailable`
- `screen_unavailable`
- `session_not_found`
- `session_stale`
- `target_not_found`
- `target_stale`
- `capture_unavailable`
- `unsupported_action`
- `batch_step_failed`

Error handling rules:

- if the game is not running or the live screen probe is inactive, return `runtime_unavailable` or `screen_unavailable`
- if a session id is unknown, return `session_not_found`
- if a session can no longer be refreshed consistently, return `session_stale`
- if a ref resolves to nothing on the latest screen, return `target_stale` or `target_not_found`
- if real capture is unavailable, fail immediately with `capture_unavailable`

## Agent Usage Pattern

Preferred path for MCP agents:

1. `moddev.ui_session_open`
2. zero or more `moddev.ui_click_ref` / `moddev.ui_hover_ref` / `moddev.ui_press_key` / `moddev.ui_type_text`
3. `moddev.ui_wait_for` only when the step needs a guard
4. `moddev.ui_screenshot` only at important checkpoints
5. `moddev.ui_trace_get` for post-run inspection

For complex flows:

1. `moddev.ui_session_open`
2. `moddev.ui_batch`
3. optional `moddev.ui_trace_get`

## Testing Strategy

### Unit Tests

- session lifecycle and stale-session handling
- ref generation and ref resolution
- batch step sequencing and stop-on-error behavior
- trace entry generation
- error code mapping for runtime unavailable, stale target, and capture unavailable
- tool registration and invocation payload coverage

### Real Runtime Validation

Use real `runClient` validation with Codex MCP:

- open a session on the title screen
- drive at least one GUI flow using the new thin tools
- save screenshots only at selected checkpoints
- inspect trace output
- confirm no fallback screenshot is returned on capture failure

## File-Level Direction

Expected implementation stays inside the existing runtime/tool area:

- extend `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- add session and trace support classes under `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/`
- reuse the existing runtime registries, live screen probe, UI drivers, and input controllers
- add tests under `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/`

## Acceptance Criteria

- agents can drive common GUI flows without repeated full query cycles
- the automation layer returns stable session refs for same-screen steps
- batch execution reduces MCP round trips for multi-step flows
- trace output is structured and useful for debugging
- failures are explicit and do not hide behind placeholder capture output
