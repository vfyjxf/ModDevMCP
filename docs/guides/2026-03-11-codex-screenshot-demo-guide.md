# 2026-03-11 Codex Screenshot Demo Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-15 00:05 CST

## Goal

- connect a Codex-style agent client
- verify readiness with `GET /api/v1/status`
- capture a real screenshot after the game is ready

## Start Order

1. generate and install the ModDevservice config for Codex
2. start `TestMod` with `runClient`
3. wait for the game to load
4. connect Codex through the generated service entry

## Recommended First Calls

1. `GET /api/v1/status`
2. `status.live_screen (via POST /api/v1/requests)`
3. `moddev.ui_capture`

If status reports `gameConnected=false`, stop and wait for the game.

For the default client flow, `TestMod` does not need an extra Gradle override block.
