# 2026-03-11 Codex Screenshot Demo

Date: 2026-03-11 21:30 CST
Updated: 2026-03-20 00:20 CST

## Goal

Capture a real screenshot after the game is ready using the HTTP operation API.

## Steps

1. `GET /api/v1/status`
2. ensure `serviceReady=true` and `gameReady=true`
3. `POST /api/v1/requests` with `ui.capture`

## Example Request

```json
{
  "operationId": "ui.capture",
  "input": {
    "source": "framebuffer",
    "mode": "full"
  }
}
```

The response includes `path` and `captureRef` for the stored image.

Use `source=framebuffer` when you want the most reliable full-screen image. `source=auto` is also acceptable now because it prefers framebuffer before narrower capture modes.

