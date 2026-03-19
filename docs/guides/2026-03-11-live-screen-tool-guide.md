# 2026-03-11 Live Screen Tool Guide

Date: 2026-03-11 17:20 CST
Updated: 2026-03-15 00:05 CST

## Purpose

Use `status.live_screen (via POST /api/v1/requests)` to ask the running Minecraft client:

- whether a screen is active
- which `screenClass` is currently open
- which UI driver is the default match
- which active UI drivers are present in `drivers[]`
- the current GUI and framebuffer dimensions
- which `includeDrivers` / `excludeDrivers` filters you may want to apply to follow-up UI reads

## Call Shape

```json
{
  "name": "status.live_screen (via POST /api/v1/requests)",
  "arguments": {
    "includeDrivers": [
      "vanilla-screen"
    ],
    "excludeDrivers": []
  }
}
```

## Typical Response

```json
{
  "active": true,
  "screenClass": "net.minecraft.client.gui.screens.TitleScreen",
  "modId": "minecraft",
  "driverId": "vanilla-screen",
  "drivers": [
    {
      "driverId": "vanilla-screen",
      "modId": "minecraft",
      "priority": 100,
      "capabilities": [
        "snapshot",
        "query",
        "capture",
        "action"
      ]
    }
  ],
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
6. call `status.live_screen (via POST /api/v1/requests)`
7. use the returned `screenClass`, `driverId`, and `drivers[]` before sending UI actions
8. if multiple drivers are active, narrow later read-only calls with `driverId`, `includeDrivers`, or `excludeDrivers`

If service connection fails, or either readiness check fails, treat the game as not ready.

For a normal consumer setup, you do not need an extra Gradle override block to use this flow.
