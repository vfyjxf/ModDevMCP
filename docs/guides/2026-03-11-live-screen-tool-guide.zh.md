# 2026-03-11 Live Screen 工具指南

Date: 2026-03-11 17:20 CST
Updated: 2026-03-15 00:05 CST

## 用途

用 `moddev.ui_get_live_screen` 向正在运行的 Minecraft 客户端查询：

- 当前是否存在活动 screen
- 当前打开的是哪个 `screenClass`
- 当前哪个 UI driver 是默认匹配
- 当前有哪些活动 UI driver，列在 `drivers[]` 里
- 当前 GUI 和 framebuffer 的尺寸
- 后续只读 UI 查询该怎么用 `includeDrivers` / `excludeDrivers` 过滤 driver

## 调用格式

```json
{
  "name": "moddev.ui_get_live_screen",
  "arguments": {
    "includeDrivers": [
      "vanilla-screen"
    ],
    "excludeDrivers": []
  }
}
```

## 典型返回

```json
{
  "active": true,
  "screenClass": "net.minecraft.client.gui.screens.TitleScreen",
  "modId": "minecraft",
  "driverId": "vanilla-screen",
  "drivers": [
    {
      "driverId": "vanilla-screen",
      "modId": "minecraft",
      "priority": 100,
      "capabilities": [
        "snapshot",
        "query",
        "capture",
        "action"
      ]
    }
  ],
  "guiWidth": 427,
  "guiHeight": 240,
  "framebufferWidth": 854,
  "framebufferHeight": 480
}
```

## 推荐用法

1. 先把生成的 MCP 配置安装到你的 MCP client
2. 启动游戏
3. 连接 agent
4. 调用 `moddev.status`
5. 只有在 `gameConnected=true` 时才继续
6. 调用 `moddev.ui_get_live_screen`
7. 在发送 UI 操作前，先利用返回的 `screenClass`、`driverId` 和 `drivers[]` 判断当前界面
8. 如果同时有多个 driver 活跃，再在后续只读调用里用 `driverId`、`includeDrivers` 或 `excludeDrivers` 收窄范围

如果 MCP 连接失败，或任一就绪检查失败，就把游戏视为尚未就绪。

普通消费者接入时，不需要为了使用这条流程而额外写 `modDevMcp {}`。
