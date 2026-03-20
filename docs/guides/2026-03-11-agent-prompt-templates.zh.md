# 2026-03-11 Agent 提示词模板

Date: 2026-03-11 17:20 CST
Updated: 2026-03-15 00:05 CST

## 用途

- 提供可直接复制的 agent 提示词
- 让不同工具的就绪检查保持一致

## 通用模板

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

## Codex / Claude Code 模板

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

## Gemini CLI / Goose 模板

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

## 给人工操作者的简版

```text
先安装生成的服务配置。
再启动 Minecraft。
等游戏加载完成。
然后让 agent 先调用 `GET /api/v1/status`。
只有在 `gameReady=true` 时才继续。
```
