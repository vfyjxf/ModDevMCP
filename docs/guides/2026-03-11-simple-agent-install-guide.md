# 2026-03-11 Simple Agent Install Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-18 09:40 CST

## Purpose

- explain the simplest consumer setup for the built-in local service
- keep agent startup aligned with exported skills and the request API
- avoid hand-maintained external server launch commands

## Consumer Project Setup

Add the published mod dependency in your NeoForge project:

```groovy
dependencies {
    implementation("dev.vfyjxf:moddevmcp:<version>") {
        transitive = false
    }
}
```

The mod exposes a loopback HTTP service from inside the running game.

## Start Order

1. start your normal game run, such as `runClient`
2. wait for the mod to finish loading
3. call the default probe `GET http://127.0.0.1:47812/api/v1/status`
4. if unavailable, use project-local fallback `<gradleProject>/build/moddevmcp/game-instances.json`
5. probe listed candidates with `GET /api/v1/status` and pick a live `baseUrl`
6. read `moddev-usage`
7. continue with `POST /api/v1/requests`

When both sides are active, client and server use separate ports.

## Exported Skills

By default the mod exports a local skill tree to:

- `~/.moddev/skills/manifest.json`
- `~/.moddev/skills/skills/moddev-usage.md`
- `~/.moddev/skills/skills/<skillId>.md`
- `~/.moddev/skills/categories/<categoryId>.md`

Agents should read the exported entry skill first when the files are available locally.

## Minimal Verification

```powershell
curl http://127.0.0.1:47812/api/v1/status
curl http://127.0.0.1:47812/api/v1/skills/moddev-usage/markdown
```

```powershell
curl -X POST http://127.0.0.1:47812/api/v1/requests `
  -H "Content-Type: application/json" `
  -d '{"requestId":"check-1","operationId":"status.get","input":{}}'
```

## Related Guides

- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-testmod-runclient-guide.md`

