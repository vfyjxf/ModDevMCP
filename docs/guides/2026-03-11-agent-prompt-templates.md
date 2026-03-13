# 2026-03-11 Agent Prompt Templates

Date: 2026-03-11 17:20 CST

Purpose:

- give operators ready-to-paste prompts
- enforce the simple rule: game first, MCP second
- keep agents from guessing whether Minecraft is ready

## Universal Template

```text
在使用 ModDevMCP 之前，先执行这个预检查流程：

1. 假设 Minecraft 必须先启动并加载完成。
2. 不要在游戏未启动时开始使用 ModDevMCP 的 UI、input、inventory、event 工具。
3. 如果 MCP 连接本身失败，立即停止，并明确告诉用户：
   “请先启动并加载游戏，再继续使用 ModDevMCP。”
4. MCP 连通后，第一步调用 `moddev.ui_get_live_screen`。
5. 只有当它成功返回时，才继续调用其他游戏相关工具。
6. 不要根据旧截图、旧日志、旧本地状态文件推断游戏已就绪。
7. 不要伪造截图、界面状态或交互结果。
```

## Codex / Claude Code Template

```text
When using ModDevMCP in this workspace:

- Do not use any ModDevMCP game tools until Minecraft is already running.
- If the MCP connection or initialization fails, stop immediately and tell the user in Chinese:
  “请先启动并加载游戏，再继续使用 ModDevMCP。”
- After MCP is available, first call `moddev.ui_get_live_screen`.
- Continue only if that call succeeds.
- Do not fabricate screenshots, UI trees, or interaction results.
- Treat MCP connection success plus `moddev.ui_get_live_screen` success as the readiness check.
```

## Gemini CLI / Goose Template

```text
Before using ModDevMCP:

1. Assume the game must already be open.
2. If the MCP connection fails, stop and say:
   “请先启动并加载游戏，再继续使用 ModDevMCP。”
3. After connecting, call `moddev.ui_get_live_screen`.
4. If that succeeds, continue.
5. Never guess game state from stale files or prior runs.
6. Never return fake capture or UI analysis when the MCP connection is unavailable.
```

## Human Operator Short Form

```text
先开游戏，等游戏加载完。
再开 agent / CLI。
agent 第一件事调用 moddev.ui_get_live_screen。
调用失败就停下提示我先把游戏开好。
```

## Suggested Placement

- paste into the system prompt of the MCP-capable agent
- or paste into the task prompt before asking it to operate Minecraft
- or add to project-specific agent instructions if the client supports persistent rules
