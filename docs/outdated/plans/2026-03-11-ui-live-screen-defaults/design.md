# UI Live Screen Defaults Design

Date: 2026-03-11 17:55 CST

## Goal

Make the primary `ui_*` tools default to the current live client screen when `screenClass` and `modId` are omitted.

## Scope

Apply the defaulting rule to the public UI tool family:

- `moddev.ui_snapshot`
- `moddev.ui_query`
- `moddev.ui_capture`
- `moddev.ui_action`
- `moddev.ui_wait`
- `moddev.ui_inspect_at`
- `moddev.ui_get_tooltip`
- `moddev.ui_get_interaction_state`
- `moddev.ui_get_target_details`
- `moddev.ui_run_intent`
- `moddev.ui_close`
- `moddev.ui_switch`

## Approach

- keep `moddev.ui_get_live_screen` as the canonical runtime probe
- reuse `ClientScreenProbe` inside `UiToolProvider`
- when a UI tool call omits `screenClass` and/or `modId`, resolve them from the current live screen metrics before driver selection
- preserve explicit arguments: if the caller passes `screenClass` or `modId`, those values still win

## Non-Goal

- do not auto-fill mouse coordinates
- do not change non-UI tools
- do not add placeholder capture fallback back into `ui_capture`
