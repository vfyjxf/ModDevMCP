# 2026-03-11 Agent Preflight Checklist

Date: 2026-03-11 17:30 CST
Updated: 2026-03-13 17:08 CST

Purpose:

- force a simple operator workflow
- require the host and game to be ready before an agent starts using game tools
- avoid speculative retries or fabricated readiness

Related:

- `docs/guides/2026-03-11-agent-prompt-templates.md`
- `docs/guides/2026-03-11-simple-agent-install-guide.md`

Recommended workflow:

1. start the host first
2. start Minecraft second
3. wait until the game finishes loading
4. call `moddev.status`
5. continue only if `gameConnected=true`
6. call `moddev.ui_get_live_screen`
7. continue only if that call succeeds

Hard rule for agents:

- if MCP connection fails, stop
- if `moddev.status` reports `gameConnected=false`, stop or wait for the user
- if `moddev.ui_get_live_screen` fails, stop
- do not call UI, input, inventory, or event tools before readiness is confirmed
- do not fabricate screenshots or pretend the game is ready
- tell the user to start or finish loading the game first

Agent preflight prompt:

```text
Use ModDevMCP only after the host MCP server is available and the Minecraft client has finished loading.

Preflight rules:
1. Do not use game tools before calling `moddev.status`.
2. If MCP connection fails, stop and tell the user: "请先启动并加载游戏，再继续使用 ModDevMCP。"
3. Continue only if `moddev.status` reports `gameConnected=true`.
4. After that, call `moddev.ui_get_live_screen`.
5. Continue only if that call succeeds.
6. Never claim that the game is ready based on old local files or previous runs.
```


