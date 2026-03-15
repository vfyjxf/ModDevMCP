# 2026-03-15 Minecraft Command Support Design

## Context

当前仓库已经具备：

- host-first gateway + backend + game runtime 架构
- `client` / `server` 双 runtime 并存
- 动态 runtime tool 沿用 gateway 既有路由

但 MCP 侧还缺一组标准化命令工具，无法：

- 查询当前 side 上有哪些可用命令
- 对半截输入做 Brigadier 补全
- 在 `server` 与 NeoForge `client` 两侧执行命令

## Decision

新增 3 个 `common` runtime tools：

- `moddev.command_list`
- `moddev.command_suggest`
- `moddev.command_execute`

命令工具不再自定义 runtime 路由参数：

- runtime 选择沿用 gateway 既有的动态 tool 路由规则
- `commandSide=client|server` 选择该 runtime 内的命令上下文
- 未连接或目标 runtime 不在线时沿用现有 gateway 错误

## Runtime Model

### Client

- 通过 `net.neoforged.neoforge.client.ClientCommandHandler`
- 使用真实 client command dispatcher
- 使用真实 client command source
- 在 `client runtime` 内同时挂载两类命令能力：
  - `commandSide=client` -> client commands
  - `commandSide=server` -> integrated server commands（如果当前单机服务端存在）
- 为避免测试和非 client 环境提前触发类解析，入口通过反射访问

### Server

- 通过 `MinecraftServer#getCommands().getDispatcher()`
- 使用 `MinecraftServer#createCommandSourceStack()`
- source 提升到 owner 级权限，保证 MCP 能看到完整命令面并执行高权限命令
- 在 `server runtime` 内只暴露 server command service

### Provider Shape

- MCP 层只保留一组工具名：
  - `moddev.command_list`
  - `moddev.command_suggest`
  - `moddev.command_execute`
- 单个 runtime 内只注册一个 `CommandToolProvider`
- provider 内部同时持有 `clientCommands` / `serverCommands`
- `commandSide` 决定 provider 调哪一类 service，而不是靠重复注册同名 tool

## Tool Contract

### `moddev.command_list`

输入：

- `commandSide?`
- `query?`
- `limit?`

输出：

- `runtimeId`
- `runtimeSide`
- `commandSide`
- `commands[]`
- `truncated`
- `totalMatched`

`commands[]` 每项包含：

- `name`
- `usage`
- `source`
- `side`
- `namespace`
- `summary`

### `moddev.command_suggest`

输入：

- `commandSide?`
- `input`
- `cursor?`
- `limit?`

输出：

- `runtimeId`
- `runtimeSide`
- `commandSide`
- `normalizedInput`
- `parseValidUpTo`
- `suggestions[]`

### `moddev.command_execute`

输入：

- `commandSide?`
- `command`

输出：

- `runtimeId`
- `runtimeSide`
- `commandSide`
- `normalizedCommand`
- `executed`
- `resultCode?`
- `messages`
- `errorCode?`
- `errorDetail?`

## Failure Model

路由层保留既有错误：

- `game_not_connected`
- `runtime_not_connected: side=...`
- `ambiguous_runtime_side: specify targetSide`

命令执行层使用结构化失败：

- `command_not_found`
- `command_parse_error`
- `command_execution_failed`
- `command_runtime_unavailable`

## Non-Goals

- 不导出完整 Brigadier 命令树
- 不做多 client / 多 server runtime 选择
- 不做额外权限系统或 MCP 侧命令沙箱
- 不采集无限长聊天日志，反馈消息限制为小数组
