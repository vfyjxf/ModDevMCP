# 2026-03-11 Agent 提示词模板

Date: 2026-03-11 17:20 CST
Updated: 2026-03-15 00:05 CST

## 用途

- 提供可直接复制的 MCP agent 提示词
- 让不同工具的就绪检查保持一致

## 通用模板

```text
When using ModDevMCP in this workspace, follow this readiness flow:

1. Assume Minecraft must already be running and fully loaded.
2. Assume the normal consumer project may not have a `modDevMcp {}` block because the plugin owns the defaults.
3. Do not use any game tool before calling `moddev.status`.
4. Continue only if `moddev.status` reports `gameConnected=true`.
5. Then call `moddev.ui_get_live_screen`.
6. Continue only if that call succeeds.
7. If MCP connection fails or either check fails, stop and tell the user the game is not ready.
8. Never fabricate screenshots, UI state, or interaction results.
9. Never infer readiness from old files, logs, or prior runs.
```

## Codex / Claude Code 模板

```text
Use ModDevMCP only after Minecraft has finished loading.

Rules:
- Do not ask for a `modDevMcp {}` block unless the user is overriding defaults.
- First call `moddev.status`.
- Continue only if `gameConnected=true`.
- Then call `moddev.ui_get_live_screen`.
- Continue only if that call succeeds.
- If MCP connection fails or a readiness check fails, stop and report that the game is not ready.
- Do not fabricate screenshots, UI trees, or action results.
```

## Gemini CLI / Goose 模板

```text
Before using ModDevMCP:

1. Assume the game must already be open and loaded.
2. Assume the normal consumer project may not define `modDevMcp {}` because defaults are automatic.
3. First call `moddev.status`.
4. Continue only if `gameConnected=true`.
5. Then call `moddev.ui_get_live_screen`.
6. If MCP connection fails or either check fails, stop.
7. Never guess the current game state from stale local files or prior runs.
```

## 给人工操作者的简版

```text
先安装生成的 MCP 配置。
再启动 Minecraft。
等游戏加载完成。
然后让 agent 先调用 `moddev.status`。
只有在 `gameConnected=true` 时才继续。
```
