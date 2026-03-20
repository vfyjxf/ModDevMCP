# 2026-03-11 Live Screen Operation Guide

Date: 2026-03-11 17:20 CST
Updated: 2026-03-20 00:10 CST

## Purpose

Use `status.live_screen` (via `POST /api/v1/requests`) to read the active client screen and core dimensions.

This operation reports:

- whether a client screen is active
- the current `screenClass`
- the current `modId`
- GUI and framebuffer dimensions

## Call Shape

```json
{
  "operationId": "status.live_screen",
  "input": {}
}
```

## Typical Response

```json
{
  "active": true,
  "screenClass": "net.minecraft.client.gui.screens.TitleScreen",
  "modId": "minecraft",
  "guiWidth": 427,
  "guiHeight": 240,
  "framebufferWidth": 854,
  "framebufferHeight": 480
}
```

## Recommended Use

1. install the generated service config into your agent client
2. start the game
3. connect the agent
4. call `GET /api/v1/status`
5. continue only if `gameReady=true`
6. call `status.live_screen` via `POST /api/v1/requests`
7. use the returned `screenClass` before sending UI actions or captures

If service connection fails, or either readiness check fails, treat the game as not ready.
