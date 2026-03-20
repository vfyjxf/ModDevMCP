# In-World UI Screenshot Design

**Date:** 2026-03-19
**Status:** Approved

## Goal

Allow `ui.screenshot` to work through HTTP when the client is in-world with `screenClass` absent, instead of rejecting the request as `screen_unavailable`.

## Problem

`moddev.ui_screenshot` currently calls `unavailableScreenResult(...)` before resolving the live driver. That helper rejects any live request whose `screenClass` is blank, even when the live client is already in-world and `fallback-region` is available with capture capability.

This blocks the intended HTTP-first workflow for world-state screenshots.

## Recommended Approach

Use the existing live driver resolution and capture pipeline, but relax only the live screenshot precondition.

Rules:

- keep session refresh behavior unchanged
- keep other live screen-dependent tools unchanged
- allow `ui.screenshot` to continue when live metrics report `inWorld=true` and `screenClass` is blank
- let the existing driver selection and capture provider matching decide whether capture is actually possible

## Testing

Add a failing regression test that reproduces the current rejection for in-world live screenshots, then make it pass by changing only the screenshot precondition.

Verify both:

- targeted unit tests
- live HTTP request against the running TestMod client while `screenClass` is blank