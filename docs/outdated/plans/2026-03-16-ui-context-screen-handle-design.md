# UiContext Screen Handle Design

**Date:** 2026-03-16

## Goal

Refactor the UI driver context surface so client-side `UiDriver` implementations can match and inspect the real live screen instance instead of relying only on `screenClass()`.

## Problem

The current `UiContext` API exposes only lightweight metadata:

- `screenClass()`
- `modId()`
- mouse coordinates
- `attributes()`

That is enough for generic or vanilla-oriented matching, but it is too weak for third-party UI libraries that can attach to arbitrary `Screen` classes and must detect themselves by inspecting the actual live screen object.

The existing downstream integration guide shows a `context.screen()` style example, but the current API does not actually provide that capability.

## Decision

Add a weakly typed screen handle to `UiContext`:

- `default Object screenHandle()`

The default implementation returns `null`.

Client runtime code that constructs the active `UiContext` should populate this field with the live `Screen` instance when available.

`UiDriver` implementations may then opt into instance-based matching and extraction:

- inspect `screenHandle()` in `matches(...)`
- derive third-party UI roots from `screenHandle()`
- fall back safely when `screenHandle()` is `null`

## Why Weak Typing

Do not expose `Screen` directly on the public `UiContext` contract.

Use `Object` instead so the public runtime API:

- stays easier to unit test without Minecraft client classes
- avoids stronger client-only type leakage into the common API surface
- keeps fake contexts and test doubles simple

This preserves the current separation where the API layer does not require a direct compile-time dependency on a concrete client screen type in every caller.

## Runtime Flow

- `UiToolProvider` continues to construct `UiContext` values and route through `registries.uiDrivers().select(context)`
- `UiDriverRegistry` selection logic remains unchanged
- built-in vanilla drivers remain compatible because they can continue matching on `screenClass()`
- third-party drivers can use `screenHandle()` for higher-fidelity matching

## Helper Layer

Add a small helper around handle access so callers do not duplicate cast logic.

The helper should:

- read `context.screenHandle()`
- expose a clear null-safe path for callers
- keep future handle-shape changes localized

This helper is intended for built-in and downstream drivers that want structured access without spreading raw casts everywhere.

## Compatibility

Compatibility rules:

- `screenHandle()` must default to `null`
- existing `UiContext` implementations must continue compiling
- existing drivers must continue working unchanged
- no MCP tool payload should expose or serialize the screen handle

This is an internal runtime matching aid, not part of external tool output.

## Scope

In scope:

- `UiContext` refactor
- live client context population
- helper utility for handle access
- docs updates to align examples with the real API
- tests covering compatibility and driver selection

Out of scope:

- redesigning the driver registry
- changing `InventoryDriver`
- forcing built-in vanilla drivers to migrate immediately
- exposing `Screen` directly in the public API

## Testing

Add tests that prove:

- old `UiContext` implementations still work with the new default method
- client-side context creation populates `screenHandle()`
- a higher-priority custom driver can match via `screenHandle()`
- vanilla/fallback driver behavior is unchanged when no custom handle-based match succeeds

