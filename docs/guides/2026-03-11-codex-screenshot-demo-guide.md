# 2026-03-11 Codex Screenshot Demo Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-15 00:05 CST

## Goal

- connect a Codex-style MCP client
- verify readiness with `moddev.status`
- capture a real screenshot after the game is ready

## Start Order

1. generate and install the ModDevMCP config for Codex
2. start `TestMod` with `runClient`
3. wait for the game to load
4. connect Codex through the generated MCP entry

## Recommended First Calls

1. `moddev.status`
2. `moddev.ui_get_live_screen`
3. `moddev.ui_capture`

If status reports `gameConnected=false`, stop and wait for the game.

For the default client flow, `TestMod` does not need a `modDevMcp {}` block.
