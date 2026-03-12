# Playwright-Style UI Automation Guide

Date: 2026-03-12 00:27 CST
Updated: 2026-03-12 08:10 CST

## Purpose

Use `moddev.ui_run_intent` as the explicit high-level entry tool for top-level screens such as `inventory`, `chat`, and `pause_menu`, then use the thin automation tools for same-screen interaction with fewer round trips than raw `ui_snapshot` and `ui_action`.

## Tools

- `moddev.ui_run_intent`
- `moddev.ui_session_open`
- `moddev.ui_session_refresh`
- `moddev.ui_click_ref`
- `moddev.ui_hover_ref`
- `moddev.ui_switch`
- `moddev.ui_press_key`
- `moddev.ui_type_text`
- `moddev.ui_wait_for`
- `moddev.ui_screenshot`
- `moddev.ui_batch`
- `moddev.ui_trace_get`

## Recommended Agent Flow

1. Call `moddev.ui_get_live_screen`.
2. If it fails, or `active = false`, stop and report that the game or target screen is not ready.
3. Call `moddev.ui_run_intent` when you need an explicit high-level entry action such as `inventory`, `chat`, or `pause_menu`.
4. Call `moddev.ui_session_open` with the current live screen once the target screen is active.
5. Use returned refs for same-screen actions with `moddev.ui_click_ref`, `moddev.ui_hover_ref`, `moddev.ui_switch`, or `moddev.ui_batch`.
6. Use `moddev.ui_batch` when several ordered steps belong to one short GUI flow.
7. Use `moddev.ui_screenshot` only at checkpoints.
8. Use `moddev.ui_trace_get` after the flow when you need structured inspection.

## Verification

Use checkpoint screenshots only where they provide value:

- before an important click
- after a screen transition
- before reporting a final result to the user

## Minimal Example

Enter a top-level screen explicitly:

```json
{
  "name": "moddev.ui_run_intent",
  "arguments": {
    "intent": "inventory"
  }
}
```

Then open a session:

```json
{
  "name": "moddev.ui_session_open",
  "arguments": {}
}
```

Then run a short batch:

```json
{
  "name": "moddev.ui_batch",
  "arguments": {
    "sessionId": "<session-id>",
    "steps": [
      { "type": "clickRef", "refId": "<ref-id>" },
      { "type": "waitFor", "refId": "<ref-id>", "condition": "appeared", "timeoutMs": 250 },
      { "type": "screenshot", "refId": "<ref-id>" }
    ]
  }
}
```

Finally inspect trace:

```json
{
  "name": "moddev.ui_trace_get",
  "arguments": {
    "sessionId": "<session-id>"
  }
}
```

## Failure Handling

- `runtime_unavailable`: the live screen probe is not available; treat the game runtime as not ready.
- `screen_unavailable`: no active screen can be used for UI automation.
- `session_not_found`: the session id is unknown.
- `session_stale`: the session can no longer be trusted without a refresh.
- `target_stale`: the old ref no longer maps to the current screen state.
- `target_not_found`: the requested ref or selector did not resolve.
- `batch_step_failed`: at least one batch step failed; inspect per-step results and trace.

## Practical Rule

Do not start with `ui_snapshot` unless you really need raw target details.

For common flows, prefer:

1. `moddev.ui_get_live_screen`
2. `moddev.ui_run_intent` for `inventory`, `chat`, or `pause_menu` when you need a high-level entry action
3. `moddev.ui_session_open`
4. `moddev.ui_click_ref`, `moddev.ui_hover_ref`, `moddev.ui_switch`, or `moddev.ui_batch`
5. `moddev.ui_screenshot` only on checkpoints
6. `moddev.ui_trace_get`
