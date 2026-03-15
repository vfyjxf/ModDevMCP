# 2026-03-15 Local World Tools Design

## Context

当前 `client runtime` 已经能通过 MCP 提供 UI、输入、截图、inventory 和命令能力，但还缺少一组面向本地单机世界的高层工具，导致 agent 不能稳定完成：

- 查看当前客户端可进入的本地存档
- 创建新的本地世界
- 让真实客户端窗口进入已有或新建的本地世界

本轮范围只覆盖本地世界，不覆盖多人服务器。

## Decision

新增 3 个仅在 `client runtime` 暴露的工具：

- `moddev.world_list`
- `moddev.world_create`
- `moddev.world_join`

工具不走纯 UI 点击流，而是采用“原生客户端 API + 真实窗口状态切换”的实现方式：

- `world_list` 读取本地世界列表数据
- `world_create` 使用客户端创建本地世界的原生流程，并在默认情况下立即进入世界
- `world_join` 使用客户端进入单机世界的原生流程
- 在必要时切换到合适 screen，保证真实客户端窗口最终进入对应界面或世界

## Why This Shape

### Option A: Pure UI automation

- 优点：完全贴近真实用户点击
- 缺点：对语言、布局、资源包和 UI 改动过于敏感

### Option B: Native client world APIs with real window transitions

- 优点：比纯 UI 更稳定，同时保持真实客户端窗口状态一致
- 缺点：需要理解 world list / create / join 的客户端调用链

### Option C: Direct save-folder manipulation

- 优点：实现看起来最短
- 缺点：不能保证客户端窗口真的进入世界，和目标不一致

采用 Option B。

## Tool Contract

### `moddev.world_list`

输入：

- 无必填参数

输出：

- `runtimeId`
- `runtimeSide`
- `worlds[]`

`worlds[]` 每项包含：

- `id`
- `name`
- `lastPlayed`
- `gameMode`
- `hardcore`
- `cheatsKnown`

说明：

- `id` 作为稳定标识
- `name` 保留给人类可读和手动调用

### `moddev.world_create`

输入：

- `name`
- `gameMode?`
  - `survival | creative | hardcore`
- `allowCheats?`
- `seed?`
- `joinAfterCreate?`

默认：

- `gameMode=survival`
- `allowCheats=false`
- `joinAfterCreate=true`

输出：

- `runtimeId`
- `runtimeSide`
- `worldId`
- `worldName`
- `created`
- `joined`

### `moddev.world_join`

输入：

- `id?`
- `name?`

规则：

- 至少提供一个
- 两者都提供时 `id` 优先
- 只提供 `name` 且匹配多个世界时返回歧义错误

输出：

- `runtimeId`
- `runtimeSide`
- `worldId`
- `worldName`
- `joined`

## Error Model

使用结构化错误码：

- `world_not_found`
- `world_name_ambiguous`
- `world_create_failed`
- `world_join_failed`
- `world_action_unavailable`
- `world_already_open`

## Runtime Model

- 仅在 `client runtime` 注册
- 只处理本地世界
- 不支持 dedicated server runtime

`world_create` / `world_join` 必须在客户端主线程执行，并以最终窗口状态成功进入世界为成功条件。

## Provider Shape

- 新增 `runtime.world` 包，承载 request/result DTO 与 service 接口
- 新增 `WorldToolProvider`
- `ClientRuntimeBootstrap` 注册 `WorldToolProvider`
- 工具 side 标记为 `client`

## Non-Goals

- 不支持多人服务器
- 不支持世界删除、重命名、导入导出
- 不支持高级创建参数，如 world preset、data packs、dimension customization
- 不走文本识别或按钮定位式 UI automation
