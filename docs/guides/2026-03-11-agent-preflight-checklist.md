# 2026-03-11 Agent Preflight Checklist

Date: 2026-03-11 17:30 CST
Updated: 2026-03-18 09:40 CST

## Purpose

- enforce a simple local-service readiness workflow
- stop agents from guessing whether the game is ready
- require explicit live checks before request execution

## Recommended Workflow

1. start Minecraft with your normal run task
2. wait for the mod to expose the local service
3. call `GET /api/v1/status`
4. continue only if `serviceReady=true`
5. if the task needs the live game, continue only if `gameReady=true`
6. read `moddev-entry`
7. read the specific category or operation skill before sending a request

## Hard Rules for Agents

- if `GET /api/v1/status` fails, stop
- if `serviceReady=false`, stop
- if the task needs client UI and `connectedSides` does not include `client`, stop
- do not invent `operationId` values; discover them from `/api/v1/operations` or skill markdown
- do not send `targetSide` unless the operation supports it
- do not omit `targetSide` after the service reports `target_side_required`
- do not fabricate screenshots, UI state, or execution results

## Short Preflight Prompt

```text
Use the local ModDevMCP HTTP service, not stale logs or old tool lists.

Preflight rules:
1. Call GET /api/v1/status first.
2. Continue only if serviceReady=true.
3. If the task needs the running game, continue only if gameReady=true.
4. Read moddev-entry, then the relevant category or operation skill.
5. Send executable work through POST /api/v1/requests.
6. Respect targetSide exactly as the service requires.
7. If any check fails, stop and report the exact failing layer.
```
