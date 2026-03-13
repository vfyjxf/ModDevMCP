# Live Screen Tool Guide

Date: 2026-03-11 17:20 CST

## Purpose

Use `moddev.ui_get_live_screen` to cheaply ask the running Minecraft client:

- whether a screen is active
- which `screenClass` is currently open
- which UI driver would handle it
- current GUI and framebuffer dimensions

## Call Shape

```json
{
  "name": "moddev.ui_get_live_screen",
  "arguments": {}
}
```

Typical response on the title screen:

```json
{
  "active": true,
  "screenClass": "net.minecraft.client.gui.screens.TitleScreen",
  "modId": "minecraft",
  "driverId": "vanilla-screen",
  "guiWidth": 427,
  "guiHeight": 240,
  "framebufferWidth": 854,
  "framebufferHeight": 480
}
```

## Recommended Use

1. Start the game first
2. Connect the MCP client
3. Call `moddev.ui_get_live_screen`
4. Use the returned `screenClass` before issuing `ui_snapshot`, `ui_capture`, or `input_action`

If the MCP connection itself fails, or this call fails immediately after connection, treat the game as not ready.
