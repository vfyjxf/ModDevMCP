# UI Driver Composition Design

**Date:** 2026-03-19

**Goal:** redesign the UI driver model so one live Minecraft UI context can expose multiple independently capable drivers, while separating UI-semantic tools from raw game input injection.

## Problem

The current runtime assumes exactly one `UiDriver` handles the active screen:

- `UiDriverRegistry.select(...)` returns a single driver
- `UiToolProvider.withDriver(...)` routes all UI tools through that one driver
- `moddev.ui_get_live_screen` reports only one `driverId`

That breaks down when one live UI context contains multiple independently meaningful UI systems at once. This is not limited to overlays. A vanilla screen can have injected mod UI, additional panels, hosted widgets, or other attached runtime surfaces that should:

- be discoverable at the same time
- expose independent `snapshot/query/action/capture` behavior
- be filterable so callers can include or exclude specific drivers

The current input split is also muddy:

- UI tools still assume a screen-driven model
- low-level key input can currently reach the game even when no UI is active
- there is no clean separation between UI-semantic actions and raw event injection

## Design Summary

Replace single-driver selection with a composition model:

- a live UI context resolves to zero or more active drivers
- each active driver remains independently capable
- tool-layer requests can target one driver or a filtered subset
- aggregated UI results are built from the filtered active set
- raw input moves into a separate low-level input tool family that bypasses UI-driver semantics

## Architecture

### 1. Active UI Composition

Introduce a composition object for the current live UI context.

Suggested shape:

- `screenClass`
- `screenHandle`
- `modId`
- `drivers[]`
- `defaultDriverId`
- request-time filter metadata
- optional capability summary

Each active driver entry should preserve:

- `driverId`
- descriptor metadata
- priority
- capabilities
- its own snapshot and related derived state

This model intentionally does not force a fixed hierarchy such as "main screen + overlays". Drivers are peers within one composed UI context.

### 2. Registry and Resolution

`UiDriverRegistry` should stop exposing only single-driver selection for live UI tools.

Instead:

- registry continues to own registration and priority ordering
- registry exposes all matching drivers for a `UiContext`
- a new composition/resolution layer turns the active matches into a stable `UiComposition`
- filtering and ambiguity handling happen at the composition/tool boundary rather than inside one selected driver

### 3. Tool Routing

All UI tools that operate on the live UI context should support consistent driver filtering:

- `driverId`
- `includeDrivers`
- `excludeDrivers`

Behavior:

- observation/query/inspection tools aggregate across the filtered active drivers
- action/capture tools may run against one driver or a filtered subset depending on the operation
- if an action cannot uniquely determine the target driver, the tool should fail explicitly rather than silently guessing

### 4. Target Identity

In a composed UI, target identity must be scoped by driver.

The runtime should treat target identity as:

- `driverId + targetId`

That applies to:

- snapshot payloads
- session refs
- wait conditions
- action routing
- inspect/query/capture results

This avoids collisions when different drivers expose the same local target id.

## Tool Contract Changes

### `moddev.ui_get_live_screen`

Current behavior returns one `driverId`.

New behavior should:

- keep `active`, `screenClass`, and size fields
- keep a compatibility `driverId` field as the default/recommended driver when available
- add `drivers[]` for all active drivers

### `moddev.ui_snapshot`

Should:

- resolve the active composition
- apply driver filters
- aggregate targets from all remaining drivers
- include per-driver metadata in the response

### `moddev.ui_query` and `moddev.ui_inspect_at`

Should:

- query each filtered driver independently
- merge results
- preserve `driverId` on every returned target

### `moddev.ui_action`

Should:

- accept explicit `driverId` targeting
- otherwise resolve target identity against the filtered active drivers
- reject ambiguous matches with a clear error such as `driver_ambiguous` or `target_ambiguous`

### `moddev.ui_capture`

Should:

- allow single-driver capture
- allow filtered multi-driver capture
- preserve clear attribution of which drivers contributed to the capture result

## Error Model

Add explicit composition-aware failures rather than collapsing everything into generic unsupported errors.

Important cases:

- `ui_unavailable` or `screen_unavailable` when no active UI composition exists
- `driver_not_found` when an explicitly requested driver is not active
- `driver_ambiguous` when routing cannot determine one driver
- `target_ambiguous` when multiple drivers expose matching targets

## Input Model

Separate UI-semantic input from raw event injection.

### UI Tools

`moddev.ui_*` remains the semantic UI layer:

- depends on an active UI composition
- should fail when no active UI is available
- should not silently degrade into raw game input injection

### Raw Input Tools

Introduce a separate low-level input family, for example `moddev.input_*`.

These tools should:

- inject raw keyboard and mouse events directly into the game
- not depend on `UiDriver`
- not require an active screen
- support press/release/click-style primitives so callers can express modifier sequences and custom event timing explicitly

This gives a clean place for low-level keyboard/mouse control without muddying UI tool semantics.

## Compatibility

Preserve old contracts where practical:

- keep existing fields when they still make sense
- treat current single-driver behavior as the degenerate case of a one-driver composition
- only require explicit `driverId` when routing is ambiguous

This should keep existing single-driver flows working while enabling multi-driver UIs.

## Testing Priorities

1. single-screen, single-driver behavior still matches current expectations
2. one live screen can resolve multiple active drivers
3. `ui_get_live_screen` reports all active drivers
4. `ui_snapshot/query/inspect/capture` respect `driverId/includeDrivers/excludeDrivers`
5. target identity remains stable across drivers
6. ambiguous action routing fails explicitly
7. raw input works without an active screen
8. UI tools no longer fall through to raw input behavior when no UI is active

## Recommended Direction

Adopt the composition model as the default architecture.

It is the smallest design that fully supports:

- multiple independent drivers on one live UI
- tool-level driver filtering
- independent per-driver capabilities
- a clean separation between UI automation semantics and raw game input injection
