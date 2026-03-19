# ModDevMCP Game Instance Discovery Design

**Date:** 2026-03-18
**Status:** Approved

## Goal

Simplify and standardize how agents find the active ModDevMCP `baseUrl` without relying on gateway-era assumptions, while supporting client and server instances running at the same time.

## Constraints

- Keep the current service-first HTTP model.
- Preserve the simple default probe at `http://127.0.0.1:47812`.
- After the default probe fails, discovery must be project-local, not global.
- Client and server instances must not share a port.
- Discovery must work for complex Gradle layouts and subprojects.
- The discovery file path must use `moddevmcp`, not `moddev`.
- A project may have at most one active `client` instance and one active `server` instance.
- Agents should prefer deriving the project from the game launch context; if they did not launch the game, the user may tell them which project to use.

## Recommended Approach

Use a two-stage discovery flow:

1. Probe the default `baseUrl` first.
2. If that fails, read a project-local registry at `<gradleProject>/build/moddevmcp/game-instances.json`, validate each candidate with `/api/v1/status`, and then route by side.

This keeps the common single-instance path fast while giving client and server dedicated, discoverable endpoints when both are live.

## Discovery Model

### Registry Location

Each Gradle project owns its own discovery file:

- `<gradleProject>/build/moddevmcp/game-instances.json`

The registry is scoped to that project only. There is no global shared registry.

### Registry Shape

The registry stores lightweight candidate data only.

```json
{
  "projectPath": "D:/ProjectDir/AgentFarm/ModDevMCP/TestMod",
  "updatedAt": "2026-03-18T12:34:56Z",
  "instances": {
    "client": {
      "baseUrl": "http://127.0.0.1:47812",
      "port": 47812,
      "pid": 12345,
      "startedAt": "2026-03-18T12:30:00Z",
      "lastSeen": "2026-03-18T12:34:55Z"
    },
    "server": {
      "baseUrl": "http://127.0.0.1:47813",
      "port": 47813,
      "pid": 12346,
      "startedAt": "2026-03-18T12:30:03Z",
      "lastSeen": "2026-03-18T12:34:55Z"
    }
  }
}
```

Rules:

- Only `client` and `server` keys are allowed under `instances`.
- New registration for a side overwrites the previous record for that side.
- The registry is not the source of truth for readiness. It only provides discovery candidates.

### Liveness Confirmation

Agents and helper flows must never trust the registry alone. After discovering candidates, they must call each instance's `/api/v1/status` and keep only live, ready instances.

## Agent Discovery Flow

1. Try `http://127.0.0.1:47812/api/v1/status`.
2. If it succeeds and satisfies the current request, use that instance.
3. If it fails, resolve the target Gradle project.
4. Read `<gradleProject>/build/moddevmcp/game-instances.json`.
5. Probe each listed candidate's `/api/v1/status`.
6. Build a live instance pool from successful probes.
7. Route the request by operation side support and optional `targetSide`.
8. Cache the chosen `baseUrl` for the current agent session.
9. If a later request fails because the instance went away, re-run discovery once before surfacing the failure.

## Routing Rules

Routing takes these inputs:

- the live instance pool for the current project
- the operation's supported sides
- optional `targetSide`

Rules:

- client-only operations must use the client instance
- server-only operations must use the server instance
- dual-side operations with explicit `targetSide` must use that side
- dual-side operations without `targetSide` may omit it only when exactly one eligible side is live
- if both eligible sides are live and `targetSide` is missing, routing must fail with `target_side_required`

This preserves the already-approved rule: only require `targetSide` when both sides are actually available for the operation.

## Failure Semantics

Standardize these failure layers:

- `service_unavailable`: no live instance is available for the current project
- `service_not_ready`: an instance exists but reports `serviceReady=false`
- `target_side_required`: both eligible sides are live and the request omitted `targetSide`
- `target_side_unavailable`: the requested side is not live for the current project
- `project_context_required`: default probing failed and the agent does not know which Gradle project to inspect

## Service Lifecycle

### Registration

After the HTTP service successfully binds its port, it writes or updates its side entry in the project registry.

### Heartbeat

While the service is running, it refreshes `lastSeen` periodically.

### Shutdown

On normal shutdown, the service removes its own side entry. On abnormal termination, the stale record is tolerated and filtered out later by live status probing.

### File Safety

Use a temp file and atomic replace when writing the registry to avoid partial JSON writes.

## Port Strategy

- Keep `47812` as the default entry probe.
- Client and server must use independent ports.
- If the preferred port is unavailable, the runtime may pick the next available port.
- The final bound port must be reflected in the registry.
- Agents should not infer side from port numbers alone.

## Documentation and Skill Updates

The standardized discovery story should live primarily in:

- `skills/moddevmcp-usage/SKILL.md`
- `Mod/src/main/resources/moddev-service/skills/moddev-usage.md`

These docs should tell agents to:

1. try the default `baseUrl`
2. fall back to `<gradleProject>/build/moddevmcp/game-instances.json`
3. confirm candidates with `/api/v1/status`
4. route by side and `targetSide`

The README may summarize the rule, but the usage skill should remain the authoritative workflow text.

## Out of Scope

- global discovery across unrelated projects
- multiple active client instances for the same project
- multiple active server instances for the same project
- port scanning across arbitrary ranges
- hidden client-first or server-first routing heuristics