# 2026-03-19 Operation API Migration Design

## Goal

Remove the legacy MCP tool layer entirely and make HTTP operations the only execution model for ModDevMCP.

## Scope

- remove `McpToolProvider`, `McpToolRegistry`, `ToolResult`, `ToolCallContext`
- remove `tools/list` and `tools/call`
- remove stdio tool-dispatch usage from the active product boundary
- migrate all built-in runtime capabilities to direct operation definitions and executors
- migrate public extension registration from tool providers to operation registrars
- allow source and binary compatibility breakage

## Current Problem

The current runtime still executes HTTP operations by looking up legacy tools and calling their handlers. That creates two public models:

1. HTTP `operationId`
2. internal `moddev.*` tool names

This split already causes visible gaps. Some capabilities exist only in the tool layer, while the HTTP surface exposes only a partial projection.

## Target Architecture

The runtime should expose one execution model only:

1. operation metadata in `OperationRegistry`
2. operation execution in a dedicated executor registry
3. HTTP endpoints as the only public invocation path

The request flow becomes:

`RequestsEndpoint -> TargetSideResolver -> OperationExecutorRegistry -> runtime service`

No intermediate tool lookup remains.

## Operation Registration Model

Each capability area contributes operations directly. Built-in domains stay grouped by category:

- status
- ui
- input
- command
- world
- hotswap

Each domain publishes:

- operation definitions
- operation executors

The old `RuntimeOperationBindings` bridge becomes a direct operation assembly layer instead of a tool-to-operation adapter.

## Public API Migration

`ModMcpApi` no longer accepts tool providers. It accepts operation registrars and direct operation contributions.

Old extension-facing types are removed:

- `registerToolProvider(...)`
- `Register*McpToolsEvent`
- `*McpToolRegistrar`

New extension-facing types replace them:

- operation registrar interfaces
- operation registration events
- direct operation contribution APIs

Extensions register definitions and handlers, not legacy tool providers.

## Runtime Bootstrap Changes

`ClientRuntimeBootstrap` and `ServerRuntimeBootstrap` stop instantiating `*ToolProvider` classes. Instead they register built-in operation contributors for each runtime side.

UI, input, command, world, game, and hotswap code remains in the runtime, but the execution entrypoints move from tool handlers to operation handlers.

## Deletion Boundary

The following legacy paths should be removed from the active build:

- `server/api/*` tool execution abstractions
- `server/runtime/McpToolRegistry`
- `server/protocol/McpProtocolDispatcher`
- `runtime/tool/*ToolProvider`
- old stdio tool-dispatch entrypoints and tests tied to `tools/call`

Resource serving may stay if still needed by HTTP capture/resource reads, but it must not depend on tool concepts.

## Testing Strategy

Migration verification must focus on the operation surface:

- unit tests for operation registration and execution
- endpoint tests for `/api/v1/operations` and `/api/v1/requests`
- skill/export tests proving every public capability is discoverable through operations
- real runtime verification that previously tool-only behavior, such as UI capture, is now reachable through HTTP operations

## Expected Outcome

After migration, there is no second execution path to keep in sync. All built-in and extension-contributed capabilities are registered once, discovered once, documented once, and executed once through the operation API.
