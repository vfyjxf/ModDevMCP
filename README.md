# ModDevMCP

ModDevMCP uses a skill-first service model for Minecraft NeoForge debugging workflows.

On branch `feat/http-skill-first-architecture`, the active product is a local HTTP service running inside `Mod` on loopback. Agents discover capabilities through skills and execute operations through HTTP requests.

## Product Boundary

- End-user runtime product: `:Mod`
- Legacy `Server` and `Plugin` modules are removed from the active build.
- Public workflow is service-first.

## Core Terms

- service
- skills
- categories
- operations
- requests
- status

## Entry Skill and Status

- Required entry skill: `moddev-usage`
- Service readiness endpoint: `/api/v1/status`
- Entry markdown explains discovery, request format, and `targetSide` routing rules

## Discovery

- Start with the default probe: `http://127.0.0.1:47812/api/v1/status`
- If unavailable, use project-local fallback: `<gradleProject>/build/moddevmcp/game-instances.json`
- Probe each candidate `baseUrl` from that file with `GET /api/v1/status`
- Client and server use separate ports when both sides are running

## Exported Skills

The service exports skills to local disk as generated projections. Exported skills are not source-of-truth files.

Typical exported layout:

- `skills/moddev-usage.md`
- `skills/<skillId>.md`
- `categories/<categoryId>.md`
- `manifest.json`

## Request Surface

- Discover metadata: `GET /api/v1/categories`, `GET /api/v1/skills`, `GET /api/v1/operations`
- Read skill markdown: `GET /api/v1/skills/{skillId}/markdown`
- Execute operation: `POST /api/v1/requests`
- Refresh exported skills: `POST /api/v1/skills/export`

## Local World Operations

- Public local-world operations are `world.list`, `world.create`, and `world.join`
- These are client-side operations even when an integrated server is already connected
- After `world.create` succeeds, reuse the returned `worldId` for later joins
- `worldId` is the local save-folder id, not just a display label

## Basic Probe

```powershell
curl http://127.0.0.1:47812/api/v1/status
```

If `serviceReady=true`, read `moddev-usage` first and continue with category or operation skills.

## Notes

- This README locks the current product boundary and service terminology.
- Legacy MCP/JSON-RPC helper scripts under 	ools/ are removed from the active workflow.
- Consumer projects only need the published `dev.vfyjxf:moddevmcp` dependency.
- Chinese version: `README.zh.md`


