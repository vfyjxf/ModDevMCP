# 2026-03-11 Codex 截图演示指南

Date: 2026-03-11 17:30 CST
Updated: 2026-03-15 00:05 CST

## 目标

- 连接一个 Codex 风格的 MCP client
- 先用 `moddev.status` 确认就绪
- 在游戏就绪后截取真实截图

## 启动顺序

1. 先为 Codex 生成并安装 ModDevMCP 配置
2. 通过 `runClient` 启动 `TestMod`
3. 等待游戏加载完成
4. 通过生成的 MCP 入口连接 Codex

## 推荐首次调用

1. `moddev.status`
2. `moddev.ui_get_live_screen`
3. `moddev.ui_capture`

如果状态返回 `gameConnected=false`，就停止并等待游戏加载完成。

默认客户端流程下，`TestMod` 不需要额外写 `modDevMcp {}`。
