# HTTP Skill-First Architecture Design

**Date:** 2026-03-17

## Goal

Replace the current gateway/tool-list-first flow with a Mod-only local HTTP service that exposes skill discovery, skill markdown, operation discovery, and request execution from inside the game runtime.

## Constraints

- Keep `Mod` as the only shipped product/module after migration.
- The service listens only on loopback and serves JSON plus markdown.
- The exported and HTTP-visible terminology must use `service`, `skills`, `categories`, `operations`, `requests`, and `status`.
- The exported flow must avoid old gateway/tool-list wording.
- There must be exactly one primary entry skill: `moddev-entry`.
- Skills are built into `Mod` as source-of-truth definitions and markdown text.
- Exported files are projections, not editable source.
- Some skills are guidance-only and have no executable operation.
- Some skills are hybrid and combine guidance plus a concrete operation.

## High-Level Shape

`Mod` owns four layers:

1. `operation registry`
2. `skill registry`
3. `http service layer`
4. `runtime/game integration layer`

The runtime/game integration layer knows how to inspect the current game state and perform actions. The operation registry exposes those capabilities through stable operation ids and request schemas. The skill registry maps operations to agent-facing markdown guidance. The HTTP service projects the registry state as JSON and markdown and accepts `curl`-style requests.

## Source Of Truth

The source of truth lives inside `Mod`:

- Java metadata for categories, skills, operations, schemas, and routing rules
- bundled markdown resources for skill bodies and category guidance
- runtime adapters that execute the actual game actions

Generated/exported skill files are derived from that internal model at runtime. They can be refreshed or re-exported, but they are never hand-edited or loaded back as authoritative state.

## Service Configuration

The service runs in-process inside the game and binds to loopback only.

Recommended defaults:

- host: `127.0.0.1`
- port: `47812`
- export root: `${user.home}/.moddev/skills`

Recommended system properties:

- `moddev.service.host`
- `moddev.service.port`
- `moddev.skill.exportRoot`

The export root is fixed by default but remains overrideable for tests and specialized setups.

## Skill Model

Each skill has explicit metadata:

- `skillId`
- `categoryId`
- `kind`
- `title`
- `summary`
- `tags`
- `operationId` (optional)
- `requiresGame`
- `markdown`

`kind` values are:

- `guidance`
- `action`
- `hybrid`

Rules:

- `guidance` skills must not require an `operationId`
- `action` skills must point to exactly one `operationId`
- `hybrid` skills combine guidance text with one primary `operationId`
- `moddev-entry` is always present and points users to category skills plus request conventions

## Category Model

Each category has:

- `categoryId`
- `title`
- `summary`
- `skillIds`
- `operationIds`

Initial categories should match the existing runtime capability split closely enough to keep migration incremental:

- `status`
- `ui`
- `command`
- `world`
- `hotswap`
- `game`
- `capture`

## Operation Model

Each operation has explicit metadata:

- `operationId`
- `categoryId`
- `title`
- `summary`
- `supportsTargetSide`
- `availableTargetSides`
- `inputSchema`
- `exampleRequest`

Operation handlers stay internal to the service. Agents only see metadata plus markdown guidance.

## Request Model

Requests use one generic execution endpoint.

Request body:

```json
{
  "requestId": "optional-client-id",
  "operationId": "ui.inspect",
  "targetSide": "client",
  "input": {
    "includeTree": true
  }
}
```

Response body:

```json
{
  "status": "ok",
  "requestId": "optional-client-id",
  "operationId": "ui.inspect",
  "resolvedTargetSide": "client",
  "result": {
    "screenTitle": "Game Menu"
  },
  "warnings": []
}
```

Error body:

```json
{
  "status": "error",
  "requestId": "optional-client-id",
  "operationId": "ui.inspect",
  "errorCode": "target_side_required",
  "errorMessage": "targetSide is required when both client and server can handle this operation",
  "details": {
    "availableTargetSides": ["client", "server"]
  }
}
```

Field naming rules:

- short
- explicit
- no overloaded terms
- no hidden defaults when routing would become ambiguous

## `targetSide` Routing Rule

`targetSide` is optional by default and becomes required only when omission would be ambiguous.

Routing behavior:

1. if an operation does not support side selection, reject any `targetSide`
2. if an operation supports side selection and exactly one eligible side is connected, resolve automatically
3. if an operation supports side selection and multiple eligible sides are connected, require `targetSide`
4. if `targetSide` is provided but disconnected or unsupported, return a structured error

This keeps the previous dual-side capability while fixing the current failure mode where omission degrades into a misleading disconnect result.

## HTTP Surface

Recommended endpoints:

- `GET /api/v1/status`
- `GET /api/v1/categories`
- `GET /api/v1/categories/{categoryId}`
- `GET /api/v1/skills`
- `GET /api/v1/skills/{skillId}`
- `GET /api/v1/skills/{skillId}/markdown`
- `GET /api/v1/operations`
- `GET /api/v1/operations/{operationId}`
- `POST /api/v1/requests`
- `POST /api/v1/skills/export`

The service returns markdown directly for skill text and JSON for metadata, discovery, status, and execution.

## Status Payload

`GET /api/v1/status` should expose at least:

- `serviceReady`
- `gameReady`
- `connectedSides`
- `entrySkillId`
- `availableCategoryIds`
- `availableOperationIds`
- `exportRoot`
- `lastDisconnect`
- `lastError`

This endpoint replaces the current need to infer readiness from host/gateway state.

## Skill Export Layout

On startup the service exports the current skill view to the local export root.

Recommended layout:

- `manifest.json`
- `skills/moddev-entry.md`
- `skills/<skillId>.md`
- `categories/<categoryId>.md`
- `indexes/skills.md`
- `indexes/categories.md`

Export rules:

- startup auto-export is enabled by default
- `POST /api/v1/skills/export` forces a refresh
- the exported tree is fully regenerated from internal state
- exported markdown always includes concrete `curl` examples where an operation exists

## Entry Skill

`moddev-entry` is the only required starting point.

It must:

- explain how to call `GET /api/v1/status`
- explain how to discover categories and skills
- explain how to read a skill markdown page
- explain how to submit a `POST /api/v1/requests` call
- explain the `targetSide` rule
- point users to category skills instead of embedding every operation inline

## Category Skills

Each category skill should:

- summarize the capability area
- list relevant operation ids
- explain readiness requirements
- show one minimal `curl` example
- link to operation-specific skills when they exist

## Operation Skills

Each operation skill should:

- state the operation id clearly
- describe required and optional input fields
- state whether `targetSide` is supported
- show one minimal request example
- list common failure codes for that operation

## Guidance-Only Skills

Guidance-only skills are valid first-class skills. They do not need an `operationId` and may only document workflow, safety rules, or environment checks.

Examples:

- entry skill
- readiness/preflight skill
- troubleshooting skill
- environment-specific installation skill

## Runtime Integration Strategy

The runtime/game integration layer should reuse the current mod-side runtime services where practical, but the new public surface should move away from tool providers and gateway dispatch.

Recommended migration strategy:

1. create operation handlers inside `Mod`
2. map current UI, command, world, hotswap, and game flows onto those handlers
3. route side-aware operations through a central resolver
4. delete the standalone gateway/server and client-config generator once the HTTP service reaches feature parity

This keeps the migration incremental while still landing on a Mod-only product.

## Error Model

Every failure should return a structured error instead of a transport-level disconnect unless the process actually died.

Required error fields:

- `errorCode`
- `errorMessage`
- `details`

Initial common error codes:

- `service_not_ready`
- `game_not_ready`
- `target_side_required`
- `target_side_not_supported`
- `target_side_not_connected`
- `operation_not_found`
- `invalid_input`
- `execution_failed`

## Documentation Rules

New entry skills, category skills, HTTP docs, and generated markdown must avoid the old gateway/tool-list vocabulary. Existing migration notes may still mention the old architecture, but only in explicitly labeled migration sections.

## Migration Impact

This architecture removes or absorbs most of the current separate host/gateway/plugin surface:

- `Server` becomes obsolete after its transport and registry responsibilities are replaced inside `Mod`
- `Plugin` becomes obsolete after generated client-config flow is replaced by HTTP skill export and direct `curl` usage
- README and guides shift from install-a-client-config instructions to service discovery plus exported skill usage

The implementation should preserve runtime capabilities first, then remove obsolete compatibility layers aggressively.

## Recommendation

Implement the HTTP skill-first architecture in `Mod` as the new primary and final architecture. Keep migration scoped around parity-first operation handlers, but do not preserve the old gateway/plugin model longer than needed to get the new service stable.
