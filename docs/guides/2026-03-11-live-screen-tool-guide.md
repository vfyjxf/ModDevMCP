# 2026-03-11 Live Screen Tool Guide

Date: 2026-03-11 17:20 CST
Updated: 2026-03-15 00:05 CST

## Purpose

Use `moddev.ui_get_live_screen` to ask the running Minecraft client:

- whether a screen is active
- which `screenClass` is currently open
- which UI driver is the default match
- which active UI drivers are present in `drivers[]`
- the current GUI and framebuffer dimensions
- which `includeDrivers` / `excludeDrivers` filters you may want to apply to follow-up UI reads

## Call Shape

```json
{
  "name": "moddev.ui_get_live_screen",
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

1. install the generated MCP config into your MCP client
2. start the game
3. connect the agent
4. call `moddev.status`
5. continue only if `gameConnected=true`
6. call `moddev.ui_get_live_screen`
7. use the returned `screenClass`, `driverId`, and `drivers[]` before sending UI actions
8. if multiple drivers are active, narrow later read-only calls with `driverId`, `includeDrivers`, or `excludeDrivers`

If MCP connection fails, or either readiness check fails, treat the game as not ready.

For a normal consumer setup, you do not need a `modDevMcp {}` block to use this flow.
