# 2026-03-14 Game Close Tool Design

## Context

`moddev.game_close` 原本按纯 client tool 实现，但新的目标不是“只关客户端”，而是：

- tool 对 agent 来说是一个 `common` tool
- gateway 必须同时支持多个 game runtime 连接到同一个 host
- 至少要能区分 `client` 和 `server` 两个 runtime
- 同一个 tool 要能按 side 精确关闭对应游戏端

当前代码里存在两个结构性限制：

1. `Server` 侧 `RuntimeRegistry` 只有单个 `activeSession`
2. `Mod` 侧只有 `ClientEntrypoint` 会自动连 host，`ServerEntrypoint` 还是空壳

所以要实现真正的双端 `moddev.game_close`，必须同时改 gateway 和 Mod runtime 连接层。

## Decision

采用“多 runtime gateway + common tool + side 定向路由”方案：

- gateway 用多 runtime 注册表维护 `client` / `server` 会话
- 动态 tool 列表按 tool name 聚合，对 agent 暴露一份 common tool descriptor
- `moddev.game_close` 输入增加可选 `targetSide`
- 指定 `targetSide` 时，精确路由到对应 runtime
- 未指定时：
  - 只有一个 side 在线，自动选择它
  - `client` 和 `server` 同时在线，返回 `ambiguous_runtime_side`
  - 没有 side 在线，返回现有未连接错误

两端运行时实现：

- `client` runtime：优雅关闭 Minecraft 客户端
- `server` runtime：优雅停止游戏服务端

## Runtime Model

### Gateway

gateway 不再只维护一个 `activeSession`，而是维护多 runtime：

- `runtimeId -> RuntimeSession`
- `runtimeId -> List<RuntimeToolDescriptor>`
- `toolName -> 聚合后的动态 tool 视图`

### Tool Exposure

同名 tool 在多个 runtime 上都存在时：

- MCP `tools/list` 只暴露一份 `name`
- descriptor 的 `inputSchema` / `outputSchema` 来自同名定义
- 对于 `moddev.game_close` 这类双端 tool，descriptor 按 `common` 处理

## Tool Contract

`moddev.game_close`：

输入：

- `targetSide?: "client" | "server"`

输出：

- `accepted: true`
- `runtimeId`
- `runtimeSide`

错误：

- `ambiguous_runtime_side: specify targetSide`
- `runtime_not_connected: side=client`
- `runtime_not_connected: side=server`
- 保留现有 runtime disconnect / timeout 类错误

## Non-Goals

- 不引入多实例 Minecraft 管理
- 不解决多个 `client` runtime 并存
- 不做“强制 kill 进程”语义
- 不修改 agent 安装方式

## Validation

需要验证四层：

1. `Server` 单测：多 runtime 注册、聚合、路由、歧义错误
2. `Server` 单测：`moddev.status` 同时报告 `client` / `server` 状态
3. `Mod` 单测：client/server runtime 都会注册并执行 `moddev.game_close`
4. 全仓 `test` 保持通过；实机双端验证单独在下一步跑
