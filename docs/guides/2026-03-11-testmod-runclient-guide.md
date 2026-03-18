# 2026-03-11 TestMod RunClient Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-18 09:40 CST

## Purpose

- run a standalone NeoForge client for real validation
- keep `TestMod` as the reference consumer project
- verify the local HTTP service and exported skills in a real game session

## Start the Client

From `TestMod`:

```powershell
cd TestMod
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat runClient --no-daemon
```

## What Users Should Expect

- the Minecraft client starts
- `ModDevMCP` loads inside the game
- the local service becomes available on loopback
- the exported skill tree is written to `~/.moddev/skills`

## Verification Steps

After the game reaches the title screen or a world:

```powershell
curl http://127.0.0.1:47812/api/v1/status
```

If the default probe fails, use project-local fallback:

- read `<gradleProject>/build/moddevmcp/game-instances.json`
- probe each listed `baseUrl` with `GET /api/v1/status`
- continue with the live `baseUrl`

When both sides are active, client and server use separate ports.

After you resolve a live `baseUrl`, read the entry skill markdown:

```powershell
curl <baseUrl>/api/v1/skills/moddev-usage/markdown
```

Optional request probes:

```powershell
curl -X POST <baseUrl>/api/v1/requests `
  -H "Content-Type: application/json" `
  -d '{"requestId":"probe-1","operationId":"status.get","input":{}}'
```

```powershell
curl -X POST <baseUrl>/api/v1/requests `
  -H "Content-Type: application/json" `
  -d '{"requestId":"probe-2","operationId":"status.live_screen","input":{}}'
```

## Agent Readiness Check

1. `GET /api/v1/status`
2. verify `serviceReady=true`
3. if the task needs live game state, verify `gameReady=true`
4. read `moddev-usage`
5. continue with request API calls

