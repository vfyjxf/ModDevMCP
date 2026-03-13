# UI Driver Playwright-Debug API Design

Date: 2026-03-12 14:20 CST

## Goal

Enhance the low-level UI driver API so the MCP can expose a Playwright-style debugging workflow for agents:

- inspect the current UI quickly
- resolve a target reliably
- perform a small action with actionability checks
- wait for visible state changes
- keep tool schemas short and low-noise

The `UiDriver` remains a runtime-facing low-level abstraction, but it becomes stronger and more reusable through default implementations.

## Context

The current stack already supports:

- live screen probing
- UI sessions with opaque refs
- ref-based click / hover / wait / screenshot / batch tools
- vanilla widget target extraction
- input-based high-level intents

What is still missing is a stronger internal driver contract for:

- target resolution semantics
- actionability checks
- wait condition evaluation
- inspect-model shaping

Without that, too much policy accumulates in `UiToolProvider`, and agents still need to consume noisy, tool-specific payloads.

## Design Principles

1. `UiDriver` stays low-level.
2. High-level MCP tools should consume a unified internal model.
3. Default implementations should make simple drivers usable immediately.
4. Drivers can override semantics where they know more about real UI structure.
5. Returned data should be concise by default and verbose only on demand.

## Driver Model

### Base Driver Responsibilities

`UiDriver` continues to own the existing base capabilities:

- `snapshot`
- `query`
- `action`
- `capture`
- `inspectAt`
- `tooltip`

These remain the hard contract every driver can implement.

### Enhanced Driver Responsibilities

`UiDriver` will gain stronger defaultable capabilities:

- `resolve`
- `checkActionability`
- `waitFor`
- `runIntent`
- `inspect`

These are still runtime-level operations. They are not MCP tools by themselves, but MCP tools should build on them.

## API Shape

### `UiTargetReference`

Unifies the ways a caller can refer to a target:

- session ref
- structured locator
- raw point fallback

This avoids duplicating resolution rules in each tool.

### `UiLocator`

A short, agent-friendly locator model:

- `role`
- `text`
- `containsText`
- `id`
- `index`
- `scopeRef`

This is intentionally smaller than the current generic selector surface. It should cover the common agent path first.

### `UiResolveRequest`

Contains:

- `reference`
- `allowMultiple`
- `includeHidden`
- `includeDisabled`

### `UiResolveResult`

Contains:

- `status`
- `matches`
- `primary`
- `errorCode`
- optional concise diagnostics

Expected stable resolution failures:

- `target_not_found`
- `target_ambiguous`
- `target_stale`

### `UiActionabilityResult`

Represents whether a target can perform a requested action.

Fields:

- `actionable`
- `visible`
- `enabled`
- `supported`
- `errorCode`
- optional `details`

Stable actionability failures:

- `target_not_visible`
- `target_disabled`
- `target_not_actionable`

### `UiWaitRequest`

Contains:

- `reference`
- `condition`
- `timeoutMs`
- `pollIntervalMs`
- `stableForMs`

Initial conditions:

- `screenIs`
- `screenChanged`
- `targetAppeared`
- `targetGone`
- `targetFocused`
- `targetHovered`
- `targetEnabled`
- `tooltipVisible`
- `textChanged`

### `UiWaitResult`

Contains:

- `matched`
- `elapsedMs`
- `matchedTarget`
- `errorCode`
- concise wait summary

### `UiInspectResult`

Internal driver-facing inspect model for high-level tools.

Contains:

- `screen`
- `screenId`
- `driverId`
- `summary`
- `targets`
- `interaction`
- optional `tooltip`

This lets high-level MCP tools return short payloads by default without rebuilding the same shape in multiple places.

## Default Implementations

Introduce a default support layer, likely `DefaultUiDriverSupport`, that provides reusable implementations for the new methods.

### Default `resolve`

Use:

- session ref lookup when available
- `query`
- `snapshot`

The default behavior should:

- resolve a single primary target when possible
- report ambiguity clearly
- avoid leaking large raw snapshots into errors

### Default `checkActionability`

Use:

- `UiTarget.state()`
- `UiTarget.actions()`

The default logic should reject:

- hidden targets
- disabled targets
- unsupported actions

### Default `waitFor`

Use polling over:

- `snapshot`
- `resolve`
- `interactionState`
- `tooltip`

This gives all drivers a useful baseline without each one implementing custom wait code.

### Default `runIntent`

Return:

- `unsupported_intent`

Driver-specific runtimes can override for supported intents such as:

- `inventory`
- `chat`
- `pause_menu`

### Default `inspect`

Compose:

- `snapshot`
- `interactionState`
- `tooltip`

Return a concise inspect model suitable for a default `ui_inspect` tool response.

## Tool-Layer Impact

The high-level MCP tools should build on the enhanced driver API:

- `moddev.ui_inspect`
- `moddev.ui_act`
- `moddev.ui_wait`
- `moddev.ui_screenshot`
- `moddev.ui_trace_recent`

Existing low-level tools stay available:

- `ui_session_open`
- `ui_session_refresh`
- `ui_click_ref`
- `ui_hover_ref`
- `ui_wait_for`
- `ui_batch`

This keeps the architecture layered:

- drivers remain runtime-level
- tool provider maps protocol input/output
- high-level tools are easier for agents

## Vanilla Driver Direction

`VanillaScreenUiDriver` should override the new default methods where it has stronger knowledge:

- `resolve`
  - text / containsText / role / id / index / scoped lookup over extracted widgets
- `checkActionability`
  - use vanilla widget active / visible / clickability semantics
- `inspect`
  - produce cleaner target lists and summary fields
- `runIntent`
  - defer to input/runtime intent support where appropriate

This is where most Playwright-like behavior will become concrete first.

## Fallback Driver Direction

Fallback drivers should keep the default support unless they have better runtime knowledge.

Important rule:

- fallback must not fake successful high-level semantics it cannot actually perform

That keeps debugging behavior honest.

## Trace Strategy

Trace remains a session/runtime concern rather than a core driver persistence responsibility.

Drivers may contribute concise diagnostics to action and inspect results, but trace storage should remain in the automation/session layer.

## Testing Strategy

### Driver-Level Tests

- default resolve behavior
- default actionability checks
- default wait condition evaluation
- default inspect composition

### Vanilla Driver Tests

- locator resolution precedence
- actionability on active vs disabled widgets
- hover/click readiness
- scoped resolution

### Tool-Level Tests

- `ui_inspect` short schema by default
- `ui_act` using `ref` and `locator`
- `ui_wait` unified conditions
- stable error codes

## Migration Strategy

1. Add the new request/result models.
2. Add default methods or support helpers to `UiDriver`.
3. Wire `UiToolProvider` to use the new driver-level helpers.
4. Keep legacy tools working while introducing higher-level tools.
5. Move Playwright-style tests to the new high-level path first.

## Acceptance Criteria

- `UiDriver` remains a low-level runtime abstraction
- the low-level abstraction becomes meaningfully stronger
- default implementations make new semantics available without forcing every driver to custom-implement them
- `UiToolProvider` becomes thinner for resolution, wait, and inspect shaping
- high-level agent-facing tools can return shorter, more stable schemas
