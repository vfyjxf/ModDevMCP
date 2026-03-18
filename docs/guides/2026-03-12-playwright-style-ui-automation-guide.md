# 2026-03-12 Playwright-Style UI Automation Guide

Date: 2026-03-12 00:27 CST
Updated: 2026-03-15 00:05 CST

## Purpose

Use the high-level UI tools in a short debug loop that feels similar to Playwright:

1. inspect
2. act
3. wait
4. screenshot
5. review trace

Use lower-level session and ref tools only when a longer same-screen flow really needs stable references.

## Preferred Tools

- `moddev.ui_run_intent`
- `moddev.ui_inspect`
- `moddev.ui_act`
- `moddev.ui_wait`
- `moddev.ui_screenshot`
- `moddev.ui_trace_recent`

## Lower-Level Fallback Tools

- `moddev.ui_session_open`
- `moddev.ui_session_refresh`
- `moddev.ui_click_ref`
- `moddev.ui_hover_ref`
- `moddev.ui_switch`
- `moddev.ui_press_key`
- `moddev.ui_type_text`
- `moddev.ui_wait_for`
- `moddev.ui_batch`
- `moddev.ui_trace_get`
- `moddev.input_action` for raw key and mouse event injection outside UI-semantic flows

## Recommended Flow

1. install the generated MCP config into your MCP client
2. call `moddev.status`
3. continue only if `gameConnected=true`
4. call `moddev.ui_get_live_screen`
5. if multiple drivers are active, choose the default `driverId` or narrow read-only calls with `includeDrivers` / `excludeDrivers`
6. call `moddev.ui_run_intent` if you need to enter a top-level screen such as `inventory`, `chat`, or `pause_menu`
7. for mixed-driver screens, call `moddev.ui_query` with `driverId` / `includeDrivers` / `excludeDrivers` to target the intended driver
8. call `moddev.ui_action` for driver-targeted actions; use `moddev.ui_inspect` / `moddev.ui_act` only when default-driver behavior is acceptable
9. call `moddev.ui_wait`
10. call `moddev.ui_screenshot` at checkpoints
11. call `moddev.ui_trace_recent` if you need a short action history

Use `moddev.input_action` instead of `moddev.ui_press_key` or `moddev.ui_type_text` when you need raw key or mouse event injection that should bypass UI-semantic screen checks.

## Minimal Example

Enter a top-level screen:

```json
{
  "name": "moddev.ui_run_intent",
  "arguments": {
    "intent": "inventory"
  }
}
```

Inspect the current UI:

```json
{
  "name": "moddev.ui_inspect",
  "arguments": {}
}
```

Click a target:

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

Wait for the next visible state:

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

Capture proof:

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

- `runtime_unavailable`
- `screen_unavailable`
- `session_not_found`
- `session_stale`
- `target_stale`
- `target_not_found`
- `batch_step_failed`
- `capture_unavailable`

## Practical Rule

For common UI debugging flows, do not start with a raw full snapshot unless you really need it.

Prefer:

1. install the generated MCP config
2. `moddev.status`
3. `moddev.ui_get_live_screen`
4. `includeDrivers` / `excludeDrivers` when multiple UI drivers are active
5. `moddev.ui_run_intent`
6. for mixed-driver screens, use `moddev.ui_query` with `driverId` / `includeDrivers` / `excludeDrivers`
7. use `moddev.ui_action` for driver-targeted actions; `moddev.ui_inspect` / `moddev.ui_act` stay default-driver-oriented
8. `moddev.ui_wait`
9. `moddev.ui_screenshot`
10. `moddev.ui_trace_recent`

For a normal consumer setup, you do not need a `modDevMcp {}` block to use this flow.
