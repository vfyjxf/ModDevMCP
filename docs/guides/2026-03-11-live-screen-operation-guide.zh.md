# 2026-03-11 Live Screen Operation 指南

日期：2026-03-11 17:20 CST
更新：2026-03-20 00:10 CST

## 目的

使用 `status.live_screen`（通过 `POST /api/v1/requests`）读取当前客户端界面与核心尺寸信息。

该 operation 返回：

- 是否存在活动屏幕
- 当前 `screenClass`
- 当前 `modId`
- GUI 与 framebuffer 的尺寸

## 调用格式

```json
{
  "operationId": "status.live_screen",
  "input": {}
}
```

## 示例返回

```json
{
  "active": true,
  "screenClass": "net.minecraft.client.gui.screens.TitleScreen",
  "modId": "minecraft",
  "guiWidth": 427,
  "guiHeight": 240,
  "framebufferWidth": 854,
  "framebufferHeight": 480
}
```

## 推荐流程

1. 把生成的 service 配置安装到 agent 客户端
2. 启动游戏
3. 连接 agent
4. 调用 `GET /api/v1/status`
5. 仅当 `gameReady=true` 继续
6. 通过 `POST /api/v1/requests` 调用 `status.live_screen`
7. 在发送 UI action 或 capture 前使用返回的 `screenClass`

如果 service 连接失败，或 readiness 检查失败，应视为游戏未就绪。
