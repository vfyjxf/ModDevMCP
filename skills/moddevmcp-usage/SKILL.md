---
name: moddevmcp-usage
description: Verify ModDevMCP installation, MCP connectivity, game readiness, and safe tool usage for Minecraft NeoForge debugging workflows. Use when an agent needs to operate ModDevMCP from Codex, Claude Code, Cursor, Cline, Windsurf, VS Code, Gemini CLI, Goose, or similar MCP-capable clients to inspect UI state, capture screenshots, query live screens, run UI actions, or troubleshoot why ModDevMCP is unavailable.
---

# ModDevMCP Usage

## Overview

Use this skill to work with ModDevMCP safely and consistently across MCP-capable clients. Confirm installation first, confirm runtime readiness second, and only then use game tools.

## Workflow

Follow this order every time:

1. Confirm the current MCP client has a `moddevmcp` server entry installed.
2. If the client was just reconfigured, tell the user a restart or MCP refresh may be required before tools appear.
3. Call `moddev.status`.
4. Continue only if `gameConnected=true`.
5. Call `moddev.ui_get_live_screen`.
6. Continue only if that call succeeds.
7. Use higher-level UI tools before lower-level tools unless the task truly needs stable refs or batching.

## Installation Check

Before using game tools, confirm all of the following:

- the MCP client shows a configured `moddevmcp` server
- the MCP client can start that server without startup errors
- the tool list includes at least `moddev.status`

If the MCP client does not show `moddevmcp`, stop and report that ModDevMCP is not installed in the current client.

If the MCP client was recently reconfigured, tell the user that some clients require a restart, workspace reload, or MCP server refresh before new tools become visible.

## Readiness Check

Treat the game as ready only after both checks succeed:

1. `moddev.status`
2. `moddev.ui_get_live_screen`

Interpret them strictly:

- if `moddev.status` fails, the MCP server is not usable yet
- if `moddev.status` returns `gameConnected=false`, the game runtime is not ready yet
- if `moddev.ui_get_live_screen` fails, client-side UI tools are not ready yet

Do not infer readiness from old screenshots, old files, old logs, or earlier runs.

## Preferred Tool Order

Use the smallest tool that matches the task.

Prefer this order for UI work:

1. `moddev.status`
2. `moddev.ui_get_live_screen`
3. `moddev.ui_run_intent` when entering a top-level screen helps
4. `moddev.ui_inspect`
5. `moddev.ui_act`
6. `moddev.ui_wait`
7. `moddev.ui_screenshot`
8. `moddev.ui_trace_recent`

Use lower-level tools such as session/ref tools, `ui_batch`, or raw snapshot/query tools only when the higher-level flow is not enough.

## Command Tool Usage

Use command tools only after readiness is confirmed with `moddev.status`.

Available tools:

- `moddev.command_list`
- `moddev.command_suggest`
- `moddev.command_execute`

Interpret `commandSide` strictly:

- `commandSide=client` means NeoForge client commands
- `commandSide=server` means server commands in the current runtime context
- in a singleplayer world, `commandSide=server` may resolve to the integrated server
- at the title screen or outside a loaded singleplayer server context, `commandSide=server` may be unavailable

Prefer this order for command work:

1. `moddev.command_list`
2. `moddev.command_suggest`
3. `moddev.command_execute`

Do not guess command availability from memory when `command_list` can verify it directly.

Minimal examples:

```json
{
  "name": "moddev.command_list",
  "arguments": {
    "commandSide": "client",
    "limit": 20
  }
}
```

```json
{
  "name": "moddev.command_execute",
  "arguments": {
    "commandSide": "server",
    "command": "time set day"
  }
}
```

## Local World Tool Usage

Use local world tools only for singleplayer saves on the connected client runtime.

Available tools:

- `moddev.world_list`
- `moddev.world_create`
- `moddev.world_join`

Preferred flow:

1. `moddev.world_list`
2. `moddev.world_join` for an existing save
3. `moddev.world_create` when a new local test world is needed

Rules:

- prefer `id` from `moddev.world_list` over visible `name`
- treat `world_create` as client-runtime-only
- `joinAfterCreate` currently needs to stay `true`
- `worldType=flat` is the supported way to request a superflat world

Minimal examples:

```json
{
  "name": "moddev.world_create",
  "arguments": {
    "name": "Flat Build World",
    "gameMode": "creative",
    "allowCheats": true,
    "worldType": "flat",
    "difficulty": "peaceful",
    "generateStructures": false
  }
}
```

```json
{
  "name": "moddev.world_join",
  "arguments": {
    "id": "dev"
  }
}
```

## Screenshot And Capture Rules

When the user asks for visual proof:

- prefer `moddev.ui_screenshot`
- save or report the real returned artifact path when available
- if capture fails, report the failure directly
- do not fabricate a screenshot summary without a real capture result

When describing the current UI, base the answer on fresh `ui_get_live_screen`, `ui_inspect`, or screenshot output from the current run.

## Failure Handling

Report failures by layer:

- installation failure: `moddevmcp` is not installed in the current MCP client
- startup failure: the MCP client cannot start the `moddevmcp` server entry
- connection failure: `moddev.status` is unavailable or errors
- game readiness failure: `moddev.status` says `gameConnected=false`
- client UI failure: `moddev.ui_get_live_screen` or later UI tools fail

When blocked, stop and tell the user exactly which layer failed and what was verified successfully before that point.

## Hard Rules

- Do not use game-specific tools before calling `moddev.status`.
- Do not continue if `gameConnected` is false.
- Do not fabricate screenshots, UI trees, interaction results, or inventory state.
- Do not claim ModDevMCP is installed unless the current MCP client actually exposes `moddevmcp`.
- Do not assume a new MCP config is live until the client refreshes or restarts if needed.
- Do not keep retrying blindly; after a clear readiness failure, report it and wait for user action or a new run state.
