# 2026-03-12 UI Automation Guide

Date: 2026-03-12 00:27 CST
Updated: 2026-03-20 00:20 CST

## Purpose

Use the current HTTP operations to inspect UI, perform actions, and capture proof images without legacy tool flows.

## Preferred Operations

- `status.live_screen`
- `ui.inspect`
- `ui.action`
- `ui.snapshot`
- `ui.capture`
- `input.action` for raw key or mouse events

## Recommended Flow

1. install the generated service config into your agent client
2. `GET /api/v1/status`
3. continue only if `gameReady=true`
4. `POST /api/v1/requests` with `status.live_screen`
5. `POST /api/v1/requests` with `ui.inspect`
6. `POST /api/v1/requests` with `ui.action`
7. `POST /api/v1/requests` with `ui.capture` for proof

When you need raw key or mouse input, use `input.action` instead of trying to infer a UI target.

## Minimal Examples

Inspect the current UI:

```json
{
  "operationId": "ui.inspect",
  "input": {}
}
```

Click a target:

```json
{
  "operationId": "ui.action",
  "input": {
    "action": "click",
    "target": {
      "role": "button",
      "text": "Create New World"
    }
  }
}
```

Capture proof (recommended default, framebuffer-first auto source):

```json
{
  "operationId": "ui.capture",
  "input": {
    "source": "auto",
    "mode": "full"
  }
}
```

Raw key input:

```json
{
  "operationId": "input.action",
  "input": {
    "action": "key_press",
    "keyCode": 69,
    "modifiers": 0
  }
}
```

## Capture Options

`ui.capture` supports:

- `source`: `auto`, `offscreen`, `framebuffer`, `render`
- `mode`: `full` or `crop`
- `target`: list of target selectors to include
- `exclude`: list of target selectors to exclude

Use `mode=crop` together with `target` for "only export" style captures.

