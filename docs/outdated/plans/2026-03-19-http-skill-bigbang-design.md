# 2026-03-19 HTTP Skill Bigbang Migration Design

## Goal

Complete a one-pass migration from legacy MCP/tool internals to a pure HTTP operation + skill architecture, while keeping the product name `ModDevMCP` unchanged.

## Scope

- remove legacy tool execution abstractions from active production paths:
  - `McpToolProvider`
  - `McpToolRegistry`
  - `ToolResult`
  - `ToolCallContext`
  - `McpProtocolDispatcher`
  - stdio `tools/list` and `tools/call` flows
- replace `operation -> toolName` bridging with direct operation executor dispatch
- migrate extension registration from `Register*McpToolsEvent` and `*McpToolRegistrar` to operation-native registration
- rename package prefix from `dev.vfyjxf.mcp` to `dev.vfyjxf.moddev`
- rename API/event/registrar symbols away from MCP wording
- clean outdated assets across repository:
  - move outdated documents/plans to `docs/outdated`
  - delete outdated scripts under `tools/` directly
- ensure README and all skill descriptions fully match the new architecture

## Architecture Boundary

The active runtime execution model must have a single public path:

`/api/v1/requests -> TargetSideResolver -> OperationExecutorRegistry -> runtime services`

No fallback MCP protocol path or tool-dispatch path remains in production code.

## Naming and API Migration

- keep project/product name: `ModDevMCP`
- migrate technical naming to operation/skill semantics:
  - `ModMcpApi` -> `ModDevApi`
  - `RegisterCommonMcpToolsEvent` -> `RegisterCommonOperationsEvent`
  - `RegisterClientMcpToolsEvent` -> `RegisterClientOperationsEvent`
  - `RegisterServerMcpToolsEvent` -> `RegisterServerOperationsEvent`
  - `CommonMcpToolRegistrar` -> `CommonOperationRegistrar`
  - `ClientMcpToolRegistrar` -> `ClientOperationRegistrar`
  - `ServerMcpToolRegistrar` -> `ServerOperationRegistrar`

Equivalent semantic renames apply for remaining MCP/tool-specific type names.

## Execution Order (Bigbang)

1. remove outdated assets and obsolete entrypoints
2. replace tool-bridged runtime execution with direct operation executors
3. perform package + symbol rename (`dev.vfyjxf.mcp` -> `dev.vfyjxf.moddev`)
4. update docs and skills to fully match new architecture

## Error Model

Do not retain compatibility shims for legacy `tools/*` methods.

Unify operation errors around:

- `invalid_request`
- `operation_not_found`
- `target_side_unavailable`
- `target_side_required`
- `operation_execution_failed`

Runtime-level unsupported scenarios must surface through operation errors with actionable messages.

## Verification Gate

Before completion, all must pass:

- compile gates (`compileJava`, `compileTestJava`)
- operation/skill/http tests for active paths
- keyword scan in production paths shows no legacy MCP tool execution artifacts:
  - `McpToolProvider`
  - `McpToolRegistry`
  - `ToolResult`
  - `ToolCallContext`
  - `tools/call`
  - `tools/list`
  - `jsonrpc`
  - `stdio mcp`
- skill discovery and markdown endpoints remain valid:
  - `GET /api/v1/skills`
  - `GET /api/v1/skills/{skillId}/markdown`

## Deliverables

- production code fully on operation + skill + HTTP model
- package namespace migrated to `dev.vfyjxf.moddev`
- outdated docs relocated under `docs/outdated`
- outdated `tools/` scripts deleted
- README/README.zh and skill docs fully rewritten to new architecture
- test suite updated to remove MCP protocol/tool assumptions

## Commit Strategy

Single migration commit for the bigbang:

`refactor: migrate from mcp tool layer to operation+skill http architecture`
