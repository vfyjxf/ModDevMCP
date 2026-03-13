# Playwright-Style UI Automation Guide

Date: 2026-03-12 00:27 CST
Updated: 2026-03-12 22:16 CST

## Purpose

Use `moddev.ui_run_intent` as the explicit high-level entry tool for top-level screens such as `inventory`, `chat`, and `pause_menu`.

Once the target UI is open, prefer the concise debug flow:

1. `moddev.ui_inspect`
2. `moddev.ui_act`
3. `moddev.ui_wait`
4. `moddev.ui_screenshot`
5. `moddev.ui_trace_recent`

Keep the older session/ref tools as a lower-level fallback when you need stable refs across several same-screen steps.

## Tools

- `moddev.ui_run_intent`
- `moddev.ui_inspect`
- `moddev.ui_act`
- `moddev.ui_wait`
- `moddev.ui_screenshot`
- `moddev.ui_trace_recent`
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
4. Call `moddev.ui_inspect` to get the current concise target view.
5. Call `moddev.ui_act` with `ref`, `locator`, or point coordinates for a small action.
6. Call `moddev.ui_wait` when the action should cause a visible state change.
7. Call `moddev.ui_screenshot` only at checkpoints or before reporting.
8. Call `moddev.ui_trace_recent` when you need a short structured view of recent session/ref actions.
9. Drop to `moddev.ui_session_open` plus ref tools only when the flow is long enough that re-resolving each step becomes noisy.

## Recommended High-Level Patterns

- Quick inspection:
  - `moddev.ui_get_live_screen`
  - `moddev.ui_inspect`
- One small interaction:
  - `moddev.ui_inspect`
  - `moddev.ui_act`
  - `moddev.ui_wait`
- Final proof:
  - `moddev.ui_screenshot`
- Low-level fallback:
  - `moddev.ui_session_open`
  - `moddev.ui_click_ref` / `moddev.ui_hover_ref` / `moddev.ui_batch`
  - `moddev.ui_trace_get`

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

Then inspect the current UI:

```json
{
  "name": "moddev.ui_inspect",
  "arguments": {}
}
```

Then act on a concise locator:

```json
{
  "name": "moddev.ui_act",
  "arguments": {
    "action": "click",
    "locator": {
      "role": "button",
      "text": "Create New World"
    }
  }
}
```

Then wait for the next visible state:

```json
{
  "name": "moddev.ui_wait",
  "arguments": {
    "condition": "targetAppeared",
    "locator": {
      "role": "button",
      "text": "Create World"
    },
    "timeoutMs": 1000,
    "pollIntervalMs": 50
  }
}
```

Finally capture proof:

```json
{
  "name": "moddev.ui_screenshot",
  "arguments": {
    "locator": {
      "role": "button",
      "text": "Create World"
    },
    "source": "auto"
  }
}
```

## Stable Failure Codes

- `runtime_unavailable`: the live screen probe is not available; treat the game runtime as not ready.
- `screen_unavailable`: no active screen can be used for UI automation.
- `session_not_found`: the session id is unknown.
- `session_stale`: the session can no longer be trusted without a refresh.
- `target_stale`: the old ref no longer maps to the current screen state.
- `target_not_found`: the requested ref or selector did not resolve.
- `batch_step_failed`: at least one batch step failed; inspect per-step results and trace.
- `capture_unavailable`: no real offscreen or framebuffer capture provider matched.

## Practical Rule

Do not start with `ui_snapshot` unless you really need the raw low-level snapshot.

For common flows, prefer:

1. `moddev.ui_get_live_screen`
2. `moddev.ui_run_intent` for `inventory`, `chat`, or `pause_menu` when you need a high-level entry action
3. `moddev.ui_inspect`
4. `moddev.ui_act`
5. `moddev.ui_wait`
6. `moddev.ui_screenshot`
7. `moddev.ui_trace_recent`

Use the session/ref family only when you need a longer same-screen script with reusable refs.
