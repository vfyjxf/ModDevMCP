# 2026-03-11 Game Service 指南

Date: 2026-03-11 17:20 CST

Updated: 2026-03-18 14:50 CST

## 用途

- 解释面向用户的运行时流程
- 说明本地 service、游戏与 agent 的关系
- 保持操作规则简单直接

## 运行时形态

- 在你的 NeoForge 工程里引入已发布的 `dev.vfyjxf:moddevmcp` 依赖
- 启动你平时使用的游戏运行任务，例如 `runClient`
- 让 mod 在游戏内部暴露 loopback HTTP service
- 先探测 `/api/v1/status`
- 再阅读 `moddev-usage`
- 最后通过 `POST /api/v1/requests` 执行 operation

## 启动流程

从你的工程里启动游戏：

```powershell
.\gradlew.bat runClient --no-daemon
```

然后直接检查本地 service：

```powershell
curl http://127.0.0.1:47812/api/v1/status
```

如果默认探测不可用，走项目级回退：

- 读取 `<gradleProject>/build/moddevmcp/game-instances.json`
- 对文件里每个 `baseUrl` 执行 `GET /api/v1/status` 探测
- 选中可用实例后继续请求

当双端同时活跃时，client 和 server 使用独立端口。

解析出可用 `baseUrl` 后，再读取入口 skill markdown：

```powershell
curl <baseUrl>/api/v1/skills/moddev-usage/markdown
```

## 第一批请求

推荐顺序：

1. `GET http://127.0.0.1:47812/api/v1/status`
2. 若不可用，读取 `build/moddevmcp/game-instances.json` 并解析可用 `baseUrl`
3. `GET <baseUrl>/api/v1/skills/moddev-usage/markdown`
4. 用 `status.get` 调用 `POST <baseUrl>/api/v1/requests`
5. 如果需要当前客户端 screen，再用 `status.live_screen` 调用 `POST <baseUrl>/api/v1/requests`

最小请求示例：

```powershell
curl -X POST http://127.0.0.1:47812/api/v1/requests `
  -H "Content-Type: application/json" `
  -d '{"requestId":"guide-1","operationId":"status.get","input":{}}'
```

## 实用规则

在 readiness 确认前，不要执行 UI、输入、背包、截图、命令、世界或热更新相关 operation。

只有在下面条件满足后才继续：

- `serviceReady=true`
- 可以读取 `moddev-usage`
- 任务依赖真实游戏状态时，`gameReady=true`

## Target Side 规则

- operation 不支持 side 选择时，不要传 `targetSide`
- 只有一个可用 side 时，不要传 `targetSide`
- 当 client 和 server 都能处理该 operation 时，传 `targetSide=client|server`
- 如果服务返回 `target_side_required`，带上明确 side 再重试

## 本地世界规则

- 本地单机存档相关能力使用 `world.list`、`world.create`、`world.join`
- 即使 integrated server 已连上，本地世界 operation 仍然按 client 侧能力处理
- `world.create` 成功后，后续进入应优先复用返回的 `worldId`
- `worldId` 是本地存档目录 id，不只是显示名称

