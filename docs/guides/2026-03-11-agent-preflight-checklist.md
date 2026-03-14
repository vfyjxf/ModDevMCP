# 2026-03-11 Agent Preflight Checklist

Date: 2026-03-11 17:30 CST
Updated: 2026-03-15 00:05 CST

## Purpose

- enforce a simple readiness workflow
- stop agents from guessing whether the game is ready
- require explicit live checks before game tools are used

## Recommended Workflow

1. make sure the generated ModDevMCP config is installed in the MCP client
2. start Minecraft with your normal run task
3. let the MCP client connect with the generated config
4. wait for the game to finish loading
5. call `moddev.status`
6. continue only if `gameConnected=true`
7. call `moddev.ui_get_live_screen`
8. continue only if that call succeeds

## Hard Rules for Agents

- if MCP connection fails, stop
- if `moddev.status` reports `gameConnected=false`, stop or wait
- if `moddev.ui_get_live_screen` fails, stop
- do not call UI, input, inventory, capture, or event tools before readiness is confirmed
- do not fabricate screenshots, UI trees, or interaction results
- do not infer readiness from stale files or prior runs

## Short Preflight Prompt

```text
Use ModDevMCP only after the MCP host is available and Minecraft has finished loading.

For a normal consumer setup, you do not need a `modDevMcp {}` block just to reach this state.

Preflight rules:
1. Do not use game tools before calling `moddev.status`.
2. Continue only if `moddev.status` reports `gameConnected=true`.
3. After that, call `moddev.ui_get_live_screen`.
4. Continue only if that call succeeds.
5. If MCP connection fails or either check fails, stop and tell the user the game is not ready.
6. Never claim the game is ready from old files, old screenshots, or earlier runs.
```
