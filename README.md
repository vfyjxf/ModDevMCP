# ModDevMCP

ModDevMCP is a skill-first service for Minecraft NeoForge debugging workflows.

The primary architecture is a local HTTP service running inside `Mod` on loopback. Agents discover capabilities through skills and execute operations through HTTP requests.

## Product Boundary

- End-user runtime product: `:Mod`
- Internal migration modules still in repo: `:Server`, `:Plugin`
- Public workflow is service-first, not host-first

## Core Terms

- service
- skills
- categories
- operations
- requests
- status

## Entry Skill and Status

- Required entry skill: `moddev-entry`
- Service readiness endpoint: `/api/v1/status`
- Entry markdown explains discovery, request format, and `targetSide` routing rules

## Exported Skills

The service exports skills to local disk as generated projections. Exported skills are not source-of-truth files.

Typical exported layout:

- `skills/moddev-entry.md`
- `skills/<skillId>.md`
- `categories/<categoryId>.md`
- `manifest.json`

## Request Surface

- Discover metadata: `GET /api/v1/categories`, `GET /api/v1/skills`, `GET /api/v1/operations`
- Read skill markdown: `GET /api/v1/skills/{skillId}/markdown`
- Execute operation: `POST /api/v1/requests`
- Refresh exported skills: `POST /api/v1/skills/export`

## Basic Probe

```powershell
curl http://127.0.0.1:47812/api/v1/status
```

If `serviceReady=true`, read `moddev-entry` first and continue with category or operation skills.

## Guides

- `docs/guides/2026-03-11-simple-agent-install-guide.md`
- `docs/guides/2026-03-11-game-mcp-guide.md`
- `docs/guides/2026-03-11-testmod-runclient-guide.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `README.zh.md`
