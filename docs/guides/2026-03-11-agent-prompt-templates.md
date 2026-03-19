# 2026-03-11 Agent Prompt Templates

Date: 2026-03-11 17:20 CST
Updated: 2026-03-15 00:05 CST

## Purpose

- provide copy-ready prompts for HTTP-capable agents
- keep readiness checks consistent across tools

## Universal Template

```text
When using ModDevMCP in this workspace, follow this readiness flow:

1. Assume Minecraft must already be running and fully loaded.
2. Assume the normal consumer project may not have an extra Gradle override block because the plugin owns the defaults.
3. Do not use any game tool before calling `GET /api/v1/status`.
4. Continue only if `GET /api/v1/status` reports `gameReady=true`.
5. Then call `status.live_screen (via POST /api/v1/requests)`.
6. Continue only if that call succeeds.
7. If service connection fails or either check fails, stop and tell the user the game is not ready.
8. Never fabricate screenshots, UI state, or interaction results.
9. Never infer readiness from old files, logs, or prior runs.
```

## Codex / Claude Code Template

```text
Use ModDevMCP only after Minecraft has finished loading.

Rules:
- Do not ask for an extra Gradle override block unless the user is overriding defaults.
- First call `GET /api/v1/status`.
- Continue only if `gameReady=true`.
- Then call `status.live_screen (via POST /api/v1/requests)`.
- Continue only if that call succeeds.
- If service connection fails or a readiness check fails, stop and report that the game is not ready.
- Do not fabricate screenshots, UI trees, or action results.
```

## Gemini CLI / Goose Template

```text
Before using ModDevMCP:

1. Assume the game must already be open and loaded.
2. Assume the normal consumer project may not define an extra Gradle override block because defaults are automatic.
3. First call `GET /api/v1/status`.
4. Continue only if `gameReady=true`.
5. Then call `status.live_screen (via POST /api/v1/requests)`.
6. If service connection fails or either check fails, stop.
7. Never guess the current game state from stale local files or prior runs.
```

## Human Operator Short Form

```text
Install the generated service config.
Start Minecraft.
Wait until the game finishes loading.
Then let the agent call `GET /api/v1/status` first.
Only continue if `gameReady=true`.
```
