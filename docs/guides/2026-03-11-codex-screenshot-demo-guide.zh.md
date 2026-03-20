# 2026-03-11 Codex 截图示例

日期：2026-03-11 21:30 CST
更新：2026-03-20 00:20 CST

## 目标

在游戏就绪后通过 HTTP operation 捕获真实截图。

## 步骤

1. `GET /api/v1/status`
2. 确认 `serviceReady=true` 且 `gameReady=true`
3. `POST /api/v1/requests` 调用 `ui.capture`

## 示例请求

```json
{
  "operationId": "ui.capture",
  "input": {
    "source": "framebuffer",
    "mode": "full"
  }
}
```

返回里会包含截图的 `path` 与 `captureRef`。

如果你要最稳的整屏真实截图，优先用 `source=framebuffer`。现在 `source=auto` 也可以作为默认值，因为它会先尝试 framebuffer。

