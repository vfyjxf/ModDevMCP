# 2026-03-11 Agent Preflight Checklist

Date: 2026-03-11 17:30 CST

Purpose:

- force a simple operator workflow
- require the game to be ready before an agent starts using ModDevMCP
- avoid teaching agents to rely on MCP-side waiting or speculative retries

Related:

- `docs/guides/2026-03-11-agent-prompt-templates.md`
- `docs/guides/2026-03-11-simple-agent-install-guide.md`

Recommended workflow:

1. start Minecraft first
2. wait until the game finishes loading
3. start the MCP client or enable the ModDevMCP MCP entry
4. if needed, generate and use `run-game-mcp-bridge.bat`
5. immediately call `moddev.ui_get_live_screen`
6. continue only if that call succeeds

Hard rule for agents:

- if MCP connection fails, stop
- if `moddev.ui_get_live_screen` fails, stop
- do not call UI, input, inventory, or event tools
- do not fabricate screenshots or pretend the game is ready
- tell the user to start or finish loading the game first

Agent preflight prompt:

```text
Use ModDevMCP only after the Minecraft client is already running.

Preflight rules:
1. Do not start ModDevMCP tool usage until the game is launched.
2. If MCP connection fails, stop and tell the user: "先启动并加载游戏，再继续使用 ModDevMCP。"
3. After ModDevMCP is available, call `moddev.ui_get_live_screen`.
4. Continue only if that call succeeds.
5. Never claim that the game is ready based on old local files or previous runs.
```
