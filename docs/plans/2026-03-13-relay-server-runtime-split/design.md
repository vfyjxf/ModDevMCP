# Relay Server Runtime Split Design

Date: 2026-03-13 16:45 CST
Status: Approved

## Goal

Replace the current game-hosted MCP architecture with a host-first architecture:

- `Server` becomes the only MCP server exposed to agents
- the Minecraft game no longer embeds or hosts an MCP server
- the game connects outward to the host as a runtime client
- multiple agents can connect to the same host and share one game runtime

## Confirmed Constraints

- the host server must be started before the game
- the game should automatically retry connection to the host in the background
- a single game runtime is enough for now
- multiple agents must be able to connect to the same game runtime through the relay
- if the game is not connected, agent queries must return explicit status and failure information
- do not depend on changing Codex MCP configuration at runtime
- dynamic tool refresh is acceptable and expected after game connect/disconnect
- game-side tools must be separated by tool scope, and pure client tools must not live in game-common scope

## Target Architecture

### Processes

There are three distinct process roles:

1. `Relay Server`
   - a standalone Java process
   - the only MCP server exposed to agents
   - owns MCP stdio transport and future transport expansion
   - listens for runtime-client connections from the game

2. `Minecraft Game Runtime`
   - a NeoForge mod process
   - does not host MCP
   - initializes game-side tool/runtime registries
   - connects to the host using a private runtime protocol

3. `Agent`
   - Codex, Claude Code, Gemini CLI, or similar
   - speaks MCP only to the relay
   - never connects directly to the game

### High-Level Flow

1. Relay starts and becomes MCP-ready.
2. Agents may connect immediately and use fixed relay-level status tools.
3. Game starts and begins background retry attempts to the relay.
4. Once connected, the game registers runtime metadata and tool descriptors.
5. Relay refreshes the visible MCP tool surface and broadcasts status changes.
6. Agent tool calls are routed through the host to the connected game runtime.

## Module Responsibilities

### `Server`

`Server` owns:

- MCP protocol dispatch
- stdio transport
- multi-agent session tracking
- runtime-client listener for the game
- request scheduling and call correlation
- dynamic tool registration visible to MCP agents
- host-owned status tools
- status notifications and refresh triggers

`Server` must not depend on Minecraft runtime classes.

### `Mod`

`Mod` owns:

- game runtime registry initialization
- game-side tool execution
- Minecraft client integration
- runtime-client connection and reconnect loop
- client-only capability providers
- future server-side game capability providers

`Mod` must not host an MCP server in the main path.

### Game-Side Scope Split

Inside `Mod`, game-side code should be organized by game scope:

- `common`
  - game-side abstractions reusable by both client and server runtimes
  - shared runtime models and dispatch surfaces
- `client`
  - client-only runtime tools and implementations
  - UI, input, capture, live screen, tooltip, client inventory automation
- `server`
  - future server-side runtime tools and implementations
  - world/entity/block/query/command state

## Tool Scope Model

Each runtime tool should carry explicit scope metadata.

### Scope Values

- `common`
- `client`
- `server`

### Required Metadata

Each tool descriptor must expose at least:

- `scope`
- `runtimeSide`
- `requiresGame`
- `mutating`
- `available`

### Scope Rules

- `client` tools are only registered when a client runtime is connected
- `server` tools are only registered when a server runtime is connected
- `common` tools are registered when their owning runtime is connected
- host-owned status tools remain available even with no game connection

## Multi-Agent Behavior

The host must support multiple simultaneous agent connections to one game runtime.

### Session Model

- one active game runtime session
- many concurrent agent sessions
- each agent performs independent MCP initialize/list/call flows

### Scheduling Model

The host owns one execution queue for game-bound calls.

First version rules:

- all game-bound calls execute serially
- mutating calls must execute serially
- read-only calls also execute serially in first version to preserve UI/state consistency

This keeps behavior deterministic for Minecraft UI and input automation.

## Relay <-> Game Private Protocol

The relay/game link should use a private JSON message protocol over TCP.

### Required Messages

- `runtime.hello`
  - runtime id
  - supported scopes
  - supported sides
  - tool descriptors
  - game/mod metadata

- `runtime.refresh`
  - indicates runtime tool availability changed

- `runtime.state`
  - reports current runtime state

- `runtime.call`
  - host requests tool execution

- `runtime.result`
  - runtime returns execution result

- `runtime.goodbye`
  - optional graceful disconnect

This protocol is private and does not need to match MCP.

## Dynamic MCP Surface

The host owns the visible MCP surface and refreshes it dynamically.

### Relay-Owned Stable Tools

These should stay available even when no game is connected:

- `moddev.status`
- optional future wait/status helpers

### Runtime-Owned Dynamic Tools

These should appear only when the runtime has connected and registered them.

Examples:

- `moddev.ui_*`
- `moddev.input_*`
- client inventory/runtime tools

When the game disconnects, host removes those tools and emits a refresh signal.

## Status and Failure Semantics

Agents must receive explicit information when the game is unavailable.

### Required Host Status Payload

Relay status responses and notifications should expose:

- `hostReady`
- `gameConnected`
- `gameConnecting`
- `connectedAgentCount`
- `queueDepth`
- `availableScopes`
- `runtimeSides`

### Required Failure Codes

- `game_not_connected`
- `game_connecting`
- `game_disconnected`
- `runtime_side_unavailable`
- `game_call_timeout`
- `runtime_protocol_error`
- `queue_overloaded`

### Behavioral Rule

- status queries must always return a real payload
- game-bound tool calls must fail explicitly with structured errors when no game is connected
- the host must never silently hang because the runtime is missing

## Stdio Non-Blocking Behavior

To reduce agent-side hanging after initialization:

- host still handles MCP requests/responses normally
- after `notifications/initialized`, host should emit a status notification such as `moddev/status`
- this gives the agent immediate output even when no runtime tools are currently available

This is an additive behavior and should not break standard MCP request/response semantics.

## Migration Strategy

### Phase 1

Add host runtime listener and host-owned status tools in `Server`.

### Phase 2

Add game-side runtime client and reconnect loop in `Mod`.

### Phase 3

Move tool registration to runtime-descriptor-based exposure and dynamic refresh.

### Phase 4

Remove the embedded game MCP host and obsolete bridge-first main path.

### Phase 5

Restructure game-side tool/runtime code into `common`, `client`, and `server` scope boundaries.

## Verification Requirements

At minimum, the implementation must prove:

- host starts without a connected game
- agent initialize works with no game
- status tool returns explicit disconnected state
- game connects later and tools become available
- multiple agents can connect to the same relay
- a disconnected game causes explicit failures instead of hanging
- dynamic refresh occurs on runtime connect/disconnect

