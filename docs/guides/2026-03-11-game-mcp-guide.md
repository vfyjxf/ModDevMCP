# 2026-03-11 Game MCP Guide

Date: 2026-03-11 17:20 CST

Updated: 2026-03-18 14:50 CST

## Purpose

- explain the user-facing runtime flow
- show how the local service, game, and agent fit together
- keep the operational rule simple

## Runtime Shape

- add the published `dev.vfyjxf:moddevmcp` dependency to your NeoForge project
- start your normal game run, such as `runClient`
- let the mod expose the loopback HTTP service from inside the game
- probe `/api/v1/status`
- read `moddev-usage`
- execute operations through `POST /api/v1/requests`

## Startup Flow

Start the game from your project:

```powershell
.\gradlew.bat runClient --no-daemon
```

Then verify the local service directly:

```powershell
curl http://127.0.0.1:47812/api/v1/status
curl http://127.0.0.1:47812/api/v1/skills/moddev-usage/markdown
```

If the default probe is unavailable, use project-local fallback:

- read `<gradleProject>/build/moddevmcp/game-instances.json`
- probe each listed `baseUrl` with `GET /api/v1/status`
- then continue with the live `baseUrl`

When both sides are active, client and server use separate ports.

## First Requests

Recommended order:

1. `GET /api/v1/status`
2. `GET /api/v1/skills/moddev-usage/markdown`
3. `POST /api/v1/requests` with `status.get`
4. `POST /api/v1/requests` with `status.live_screen` if you need the current client screen

Minimal request example:

```powershell
curl -X POST http://127.0.0.1:47812/api/v1/requests `
  -H "Content-Type: application/json" `
  -d '{"requestId":"guide-1","operationId":"status.get","input":{}}'
```

## Practical Rule

Do not use UI, input, inventory, capture, command, world, or hotswap operations before readiness is confirmed.

Continue only if:

- `serviceReady=true`
- `moddev-usage` is reachable
- `gameReady=true` when the task depends on live game state

## Target Side Rule

- omit `targetSide` when the operation does not support side selection
- omit `targetSide` when exactly one eligible side is connected
- send `targetSide=client|server` when both sides can handle the operation
- if the service returns `target_side_required`, retry with an explicit side

## Local World Rule

- use `world.list`, `world.create`, and `world.join` for local singleplayer saves
- treat local world operations as client-side operations even when an integrated server is already connected
- after `world.create` succeeds, reuse the returned `worldId` for later joins
- `worldId` is the local save-folder id, not just a display label

