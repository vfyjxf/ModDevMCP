# Game MCP Design

Date: 2026-03-11 17:05 CST

## Goal

Simplify ModDevMCP from a `stable server + game backend agent` architecture to a `game MCP` architecture.

Target outcome:

- `runClient` starts Minecraft and the MCP endpoint inside the same game process
- external MCP clients connect only after the game is already running
- agent usage is constrained by prompt/checklist, not by auto-start wrappers
- stable-server-specific startup, reconnect, and lifecycle layers are removed from the primary path

## Recommended Architecture

### Runtime model

- `Server` remains the reusable MCP protocol/transport library
- `Mod` remains the Minecraft runtime/tool implementation
- the Minecraft client process directly hosts one MCP socket server
- an optional lightweight stdio bridge may proxy stdio to that socket, but it does not start Minecraft or any extra MCP backend

### Startup model

- `ClientEntrypoint` should initialize runtime providers and start the in-process MCP socket host
- there is no `stable server`
- there is no separate backend agent
- there is no server-side readiness resource for a detached runtime

### Client model

- Codex/CLI should use a connect-only bridge or direct socket-aware tooling
- prompts/guides must require:
  - start game first
  - confirm MCP endpoint is reachable
  - then use game tools

## Naming Direction

The current names over-emphasize the old embedding/stable distinction.

Preferred naming:

- `ClientRuntimeBootstrap` -> replace with a simpler game MCP runtime/host bootstrap
- `EmbeddedModDevMcpStdioMain` -> rename toward `GameMcpBridgeMain` or equivalent connect-only naming
- remove `Stable*` naming from the primary documented flow

## Migration Scope

### Keep

- core MCP protocol/registry/SDK glue in `Server`
- tool/resource registration model
- all runtime UI/input/inventory implementations in `Mod`
- direct `runClient` workflow

### Remove from primary architecture

- stable server socket host as the default entrypoint
- backend attach session model
- runtime status indirection created only for detached stable-server mode
- auto-start stable server logic

### Downgrade or delete

- stable launch/install scripts
- stable-server-specific docs and prompts
- TestMod wiring that depends on `:Server:installLocalStableServer`

## Validation

The simplified architecture is correct when:

- `runClient` starts a game-local MCP endpoint
- Codex can connect after game startup without any stable-server process
- existing UI/input/capture tools still work
- documentation points users to “start game first, then connect”
