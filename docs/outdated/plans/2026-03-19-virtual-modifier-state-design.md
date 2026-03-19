# Virtual Modifier State Design

**Date:** 2026-03-19
**Status:** Approved

## Goal

Allow agent-driven modifier keys to behave like real held modifiers across subsequent input events, so both NeoForge keybinding modifier checks and vanilla `Screen.*Down()` queries observe the same effective state.

## Problem

The current raw keyboard path only simulates combo keys as short event sequences:

- press modifier key
- press and release the main key
- release modifier key

That is enough for some event-driven handlers, but it does not create a persistent modifier state. Code that directly polls modifier state, such as:

- `Screen.hasShiftDown()`
- `Screen.hasControlDown()`
- `Screen.hasAltDown()`
- `net.neoforged.neoforge.client.settings.KeyModifier.isActive(...)`

can miss agent-sent modifiers once the synthetic event sequence finishes.

The user wants a virtual modifier model with these semantics:

- explicit modifier `key_down` keeps the modifier active until matching `key_up`
- virtual modifier state merges with the real physical keyboard state
- `key_click` and `key_press` keep their current one-shot combo semantics and do not persist modifier state

## Recommended Approach

Add a small runtime-owned virtual modifier state and expose it through focused mixins at the modifier query layer.

This means:

1. maintain a persistent virtual modifier set for agent-sent modifier `key_down` / `key_up`
2. compute effective modifiers as `physical OR virtual OR one-shot command modifiers`
3. patch vanilla and NeoForge modifier query helpers so polling code sees the merged state

This covers both event-driven and query-driven input paths without trying to fake low-level GLFW internals globally.

## State Model

Introduce a runtime utility such as `VirtualModifierState` that tracks four logical modifiers:

- `SHIFT`
- `CONTROL`
- `ALT`
- `SUPER`

Rules:

- left and right physical key codes map to the same logical modifier
- repeated `key_down` and repeated `key_up` are idempotent
- unknown keys do not affect virtual modifier state
- virtual state is additive with physical state, not a replacement

The logical `SUPER` state is stored even if vanilla only exposes public helpers for shift/control/alt, because NeoForge control logic on macOS can map to super.

## Input Semantics

### Persistent modifier updates

Only explicit modifier `key_down` and `key_up` mutate persistent virtual state.

Examples:

- `key_down(left_shift)` turns virtual shift on
- later `key_down(A)` should be dispatched with shift active even if the command carries no `modifiers`
- `key_up(left_shift)` turns virtual shift off

### One-shot combo modifiers

`key_click` and `key_press` commands may still include `modifiers`, but those modifiers stay request-scoped only.

They should:

- affect the dispatched event sequence for that call
- be visible to modifier queries during that call
- not remain active after the call returns

This preserves current combo-key behavior while adding the new persistent model for explicit modifier holds.

### Effective modifier computation

Every dispatched input event should derive its modifier bits from a single merged view:

- real physical modifier state
- persistent virtual modifier state
- current command's one-shot modifier bits

This merged view should be used for keyboard and mouse events so held virtual modifiers also affect click-and-drag or other modifier-sensitive mouse interactions.

## Query Integration

Patch the high-level query points instead of lower GLFW internals.

### Vanilla

Mixin into `net.minecraft.client.gui.screens.Screen` static modifier helpers:

- `hasShiftDown()`
- `hasControlDown()`
- `hasAltDown()`

Each helper should return:

- original result OR matching virtual modifier state

### NeoForge

Mixin into `net.neoforged.neoforge.client.settings.KeyModifier.isActive(...)`.

Behavior:

- `SHIFT` uses original result OR virtual shift
- `ALT` uses original result OR virtual alt
- `CONTROL` uses original result OR virtual control, plus virtual super where NeoForge or platform semantics treat super as control
- `NONE` is not directly rewritten; it should inherit the updated modifier activity checks naturally

## Lifecycle and Reset

`VirtualModifierState` should expose an explicit `clear()` for obvious lifecycle boundaries.

Recommended reset points:

- client startup/bootstrap
- client shutdown
- any already-available client session reset boundary that clearly ends one automation context and starts another

The state should not silently expire on timers. If the agent forgets to send `key_up`, the held modifier remains active until an explicit reset or release.

## Mixin Integration Scope

The repository does not currently expose an obvious existing Mixin configuration in `Mod`, so this design includes a minimal Mixin bootstrap for the client-side modifier patches only.

The mixin integration should stay narrow:

- only add the config needed for these modifier query hooks
- do not broaden the patch surface to unrelated UI or input systems

## Testing Priorities

1. virtual modifier state maps left/right keys to one logical modifier
2. repeated modifier presses and releases are idempotent
3. explicit modifier `key_down` persists across later non-modifier events
4. `key_click` / `key_press` modifiers remain one-shot and do not leak into persistent state
5. merged modifier bits are used consistently by input dispatch helpers
6. query merge logic returns true when either physical or virtual modifier state is active
7. NeoForge control handling still respects macOS-style super/control behavior
8. reset hooks clear stale virtual modifier state

## Out of Scope

- faking raw GLFW key tables globally
- introducing persistent virtual state for non-modifier keys
- redefining existing text input shortcuts
- widening the modifier patch surface beyond vanilla `Screen` helpers and NeoForge `KeyModifier`
