# 2026-03-11 游戏 MCP 指南

Date: 2026-03-11 17:20 CST
Updated: 2026-03-15 00:05 CST

## 用途

- 解释面向用户的运行流程
- 说明 host、游戏、agent 之间如何协作
- 把操作规则保持得足够简单

## 运行形态

- 普通消费者接入时，只需要应用插件并添加已发布的 mod 依赖，不需要额外写 `modDevMcp {}`
- 用 `createMcpClientFiles` 先生成并安装一次 MCP client 配置
- 先把生成出来的 MCP client 配置安装到你的 MCP client
- 再启动你平时使用的游戏运行任务，例如 `runClient`
- 让游戏自动连接到 host
- 通过生成出来的 MCP 入口建立连接
- 在任何游戏相关工具之前先调用 `moddev.status`

## 启动流程

在你的工程里启动游戏：

```powershell
.\gradlew.bat runClient --no-daemon
```

然后用生成好的 ModDevMCP 配置连接你的 MCP client。MCP client 会根据这个配置自动启动 host 入口。

## Agent 首次调用

推荐顺序：

1. `moddev.status`
2. `moddev.ui_get_live_screen`

只有在下面两点都满足时才继续：

- `gameConnected=true`
- live screen 调用成功

## 实际规则

在确认就绪之前，不要使用 UI、input、inventory 或 capture 工具。

如果连接失败，或最开始的检查失败，就把游戏视为尚未就绪。

普通消费者接入时，不需要为了使用这条运行流程而额外写 `modDevMcp {}`。
