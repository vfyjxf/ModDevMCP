---
name: moddevmcp-usage
description: Use when an agent needs to operate a local ModDevMCP game session through its built-in HTTP service, exported skills, and request API for Minecraft debugging or automation.
---

# ModDevMCP Usage

## Overview

ModDevMCP is a loopback HTTP service exposed by the game mod itself. Resolve a live baseUrl first, then read skills and execute operations through `POST /api/v1/requests`.

## Required Flow

1. Try the default probe `GET http://127.0.0.1:47812/api/v1/status`.
2. If the default probe fails, read `<gradleProject>/build/moddevmcp/game-instances.json`.
3. Probe each candidate `baseUrl` from the registry with `GET /api/v1/status` and keep only live instances.
4. Continue only if `serviceReady=true`.
5. After you have a live `baseUrl`, read the exported entry skill from `~/.moddev/skills/skills/moddev-usage.md` when available, or fetch `GET <baseUrl>/api/v1/skills/moddev-usage/markdown`.
6. Treat client UI work as ready only if `gameReady=true` and `connectedSides` includes `client`.
7. Discover available skills and operations before guessing names:
   - `GET /api/v1/categories`
   - `GET /api/v1/skills`
   - `GET /api/v1/operations`
8. Prefer reading the specific skill markdown before issuing a request.

## Discovery Rules

- `moddev-usage` is the required starting skill.
- Some exported skills are guidance-only. They explain workflow and do not map to an executable operation.
- Category skills summarize a capability area and point to operation skills.
- Operation skills show the exact `operationId`, input shape, and a minimal `curl` example.

## Request Rules

Send all executable work through `POST /api/v1/requests`.

Do not control the game through OS-level or shell-level input injection.

- do not use PowerShell, Windows APIs, or external automation helpers to send keyboard input
- do not use PowerShell, Windows APIs, or external automation helpers to move or click the mouse
- do not treat simulated user input outside ModDevMCP as an acceptable fallback
- if a game interaction is needed, use the exposed ModDevMCP operations and the skill guidance for them

Envelope fields:

- `requestId`
- `operationId`
- `targetSide`
- `input`

Interpret `targetSide` strictly:

- omit it when the operation does not support side selection
- omit it when exactly one eligible side is connected
- send it when multiple eligible sides are connected
- if the service returns `target_side_required`, retry with an explicit side

`targetSide` is required only when both eligible sides are live for that operation.

Minimal example:

```bash
curl -X POST http://127.0.0.1:47812/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{"requestId":"check-1","operationId":"status.get","input":{}}'
```

## Preferred Order

For any new session:

1. `GET http://127.0.0.1:47812/api/v1/status`
2. if needed, read `<gradleProject>/build/moddevmcp/game-instances.json` and probe listed candidates with `GET /api/v1/status`
3. pick a live `baseUrl`
4. `GET <baseUrl>/api/v1/skills/moddev-usage/markdown`
5. read the relevant category or operation skill
6. `POST <baseUrl>/api/v1/requests`

For UI work:

1. verify `connectedSides` includes `client`
2. use `status.live_screen`
3. use `ui.inspect`
4. use `ui.snapshot` or `ui.action` only when needed

UI interactions must stay inside ModDevMCP. Do not bypass `ui.*` operations with shell scripts or system automation.

For commands:

1. `command.list`
2. `command.suggest`
3. `command.execute`

For local worlds:

1. `world.list`
2. `world.join` with `worldId` when re-entering an existing save
3. `world.create` when a fresh singleplayer world is required
4. after `world.create` succeeds, treat the returned `worldId` as the stable save id for later calls

For hotswap:

1. `hotswap.reload`
2. if it returns a structured execution error, fix the code or restart the game instead of blindly retrying

## Failure Handling

Report the exact failure layer:

- service missing: `/api/v1/status` is unavailable
- service not ready: `serviceReady=false`
- game not ready: `gameReady=false`
- side unavailable: requested side is absent from `connectedSides`
- invalid request: `errorCode=invalid_request`
- execution failure: use the returned `errorCode` and `errorMessage`

For local world failures:

- `world_not_found` means an existing target save was not resolved
- `world_name_ambiguous` means the visible name matched multiple saves
- `world_create_failed` or `world_join_failed` means the game did not complete the requested transition
- if the game visibly entered the new world but the response reports `world_not_found`, treat that as a runtime bug, not a caller mistake

Do not claim a skill or operation exists unless it is visible from the current service or exported skill tree.