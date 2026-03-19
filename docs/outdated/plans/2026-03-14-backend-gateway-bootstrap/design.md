# Backend Gateway Bootstrap Design

Date: 2026-03-14 09:05 CST
Status: Approved

## Goal

Replace the current "stdio host owns everything" startup path with a stable backend plus a lightweight stdio gateway:

- `backend` becomes the only long-lived local state owner
- `stdio gateway` becomes the MCP-facing process started by Codex and similar tools
- `gateway` automatically starts `backend` when needed
- `Mod` only reconnects to `backend`
- plugin-generated files become the default supported installation path

## Confirmed Constraints

- the user should not need to manually compose Java commands
- Codex, Claude Code, Cursor, Cline, Gemini CLI, Goose, and similar agents should install from generated files
- `gateway` should auto-start `backend`
- the game should not auto-start `backend`
- the game should always retry `backend` in the background
- `moddev.status` must report explicit state instead of hanging
- if `backend` is unavailable, MCP startup should fail clearly instead of silently blocking
- the final flow should work with `gradlew runClient`

## Target Process Model

There are three runtime roles:

1. `backend`
   - local long-lived Java process
   - owns runtime listener, runtime registry, queue, agent-visible state
   - survives agent restarts and game restarts

2. `stdio gateway`
   - short-lived per-agent MCP stdio process
   - auto-starts or attaches to `backend`
   - forwards MCP protocol requests to `backend`
   - does not own the runtime listener

3. `game runtime`
   - NeoForge process
   - auto-reconnects to `backend`
   - reports runtime descriptors and executes real game tools

## Responsibility Split

### `Server`

`Server` should expose two explicit bootstrap paths:

- `ModDevMcpBackendMain`
  - starts backend listener
  - serves gateway/backend private protocol
  - remains alive until terminated

- `ModDevMcpGatewayMain`
  - starts backend if missing
  - waits until backend is ready
  - serves MCP stdio
  - proxies MCP calls to backend

`Server` should stop treating stdio EOF as the lifecycle owner of backend.

### `Plugin`

The plugin should generate:

- backend launch args and scripts
- gateway launch args and scripts
- client install snippets for Codex, Claude Code, Cursor, Cline, Gemini CLI, Goose, and VS Code

Generated MCP client files should point to the gateway launcher, not to a raw `java @args` invocation.

### `Mod`

`Mod` keeps only:

- runtime bootstrap
- runtime hello / runtime refresh / runtime result handling
- reconnect loop to backend

`Mod` should not host MCP stdio and should not own backend startup.

## Startup Flow

1. Agent starts `stdio gateway`.
2. Gateway probes backend endpoint.
3. If backend is not reachable:
   - gateway starts backend using generated backend launcher
   - gateway waits for backend ready
4. Gateway completes MCP startup only after backend is ready.
5. User starts `runClient`.
6. Game connects or reconnects to backend in the background.
7. Agent calls `moddev.status` and sees current backend/game state.

## Failure Semantics

### Gateway Startup

If gateway cannot start or reach backend in time, startup should fail with a clear backend-specific error:

- `backend_unavailable`
- `backend_start_failed`
- `backend_start_timeout`

### Status Tool

`moddev.status` should distinguish:

- backend unavailable
- backend ready, game disconnected
- backend ready, game connecting
- backend ready, game connected

### Game Tool Calls

When backend is ready but no game is connected, game-bound tools should fail explicitly:

- `game_not_connected`
- `game_call_timeout`
- `runtime_unavailable`

## Installation Model

The supported install path should be:

1. apply plugin
2. run generated client file task
3. install the generated Codex or other agent config
4. start agent
5. gateway auto-starts backend
6. run `gradlew runClient`

This makes the generated files the primary product rather than a debugging artifact.
