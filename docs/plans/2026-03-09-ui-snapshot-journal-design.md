# UI Snapshot Journal And Interaction Resolver Design

**Date:** 2026-03-09

**Goal:** extend the built-in UI MCP tools so actions and captures return stable snapshot references and so default interaction state rules are provided through a runtime SPI instead of being hardcoded in each driver.

## Scope

This slice only covers the built-in UI runtime in `Mod` and the built-in MCP UI tools exposed through `Server`.

Included:

- `ui_capture` returns a stable `snapshotRef` and a structured captured snapshot payload
- `ui_action`, `ui_open`, `ui_close`, and `ui_switch` return `preSnapshotRef`, `postSnapshotRef`, and `postActionSnapshot`
- built-in drivers compute default focused, selected, hovered, and active targets through a resolver SPI
- snapshot storage is in-memory only

Excluded:

- persistent snapshot storage
- binary image transport
- cross-process snapshot sync
- third-party resolver implementations beyond the built-in vanilla and fallback ones

## Runtime Additions

### `UiSnapshotJournal`

The runtime stores snapshots in memory under a generated `snapshotRef`.

Behavior:

- key snapshots by `driverId`, `screenClass`, and `modId`
- generate sequential refs such as `ui-1`, `ui-2`
- store the full `UiSnapshot`
- expose `record(UiContext, UiSnapshot)` and `latest(UiContext, String driverId)`

This makes snapshot creation reusable across tools instead of rebuilding ad hoc payloads in `UiToolProvider`.

### `UiInteractionStateResolver`

Drivers should no longer hardcode default interaction ids in protected methods.

Behavior:

- resolvers are registered in runtime, just like UI drivers
- each resolver declares a `driverId`, priority, and a `matches(UiContext, List<UiTarget>)` predicate
- resolution returns default focused, selected, hovered, and active ids plus selection source

The built-in runtime provides:

- a vanilla-container resolver
- a vanilla-screen resolver
- a fallback-region resolver

If no resolver matches, the runtime falls back to an empty default state.

## Tool Behavior

### `moddev.ui_capture`

Returns:

- `driverId`
- `mode`
- `capturedTargets`
- `excludedTargets`
- `snapshotRef`
- `capturedSnapshot`

`capturedSnapshot` is a structured `UiSnapshot` projection, not image bytes.

### `moddev.ui_action`

Returns:

- existing action metadata
- `preSnapshotRef`
- `postSnapshotRef`
- `postActionSnapshot`

`ui_open`, `ui_close`, and `ui_switch` reuse the same shape because they delegate into `ui_action` semantics.

## Testing Strategy

Add red-green tests for:

- `ui_capture` includes `snapshotRef` and `capturedSnapshot`
- `ui_switch` returns distinct pre/post refs and a post snapshot focused on the switched target
- `ui_close` returns a post snapshot with no targets
- `ui_open` returns a post snapshot restored from resolver defaults
- built-in interaction state still reflects resolver defaults after open

## Rationale

This keeps the current server/mod split intact while moving snapshot lifecycle and default interaction logic into reusable runtime services. That gives a direct path for later third-party UI frameworks to register their own resolvers and for additional tools to consume stable snapshot references without redesigning the API.
