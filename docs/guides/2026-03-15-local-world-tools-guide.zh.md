# 本地世界操作指南

Date: 2026-03-15

Updated: 2026-03-18 14:50 CST

## 用途

- 说明本地单机世界相关的公共 HTTP operation
- 给出正确的 request envelope 与 side 规则
- 说明 `worldId` 的含义和常见失败模型

## 公共 Operation

- `world.list`
- `world.create`
- `world.join`

这些都是通过 `POST /api/v1/requests` 调用的公共 service operation。

mod 内部运行时 provider 仍然使用 `moddev.world_list`、`moddev.world_create`、`moddev.world_join`，但 agent 在正常使用时应优先使用公共 operation id，而不是内部工具名。

## 范围

- 只支持本地单机世界
- 只处理客户端侧的世界流转
- 不支持多人服务器
- 不支持删除、重命名、导入或导出世界

## 必要流程

1. 调用 `GET /api/v1/status`
2. 确认 `serviceReady=true`
3. 在 create 或 join 之前确认 `gameReady=true`
4. 需要进入已有存档时先调用 `world.list`
5. 进入已有世界时优先用 `world.join` + `worldId`
6. 需要新测试世界时调用 `world.create`

## Target Side 规则

- `world.*` operation 只支持 client
- 当当前会话里只有一个可用 side 时，可以省略 `targetSide`
- 显式传 `targetSide:"client"` 也是合法的，脚本里通常更清晰

## 请求示例

列出本地世界：

```json
{
  "requestId": "world-list-1",
  "operationId": "world.list",
  "input": {}
}
```

创建并进入一个 creative 测试世界：

```json
{
  "requestId": "world-create-1",
  "operationId": "world.create",
  "targetSide": "client",
  "input": {
    "name": "MCP Test World",
    "gameMode": "creative",
    "allowCheats": true,
    "worldType": "default",
    "difficulty": "normal",
    "generateStructures": true
  }
}
```

创建并进入一个超平坦建造世界：

```json
{
  "requestId": "world-create-flat-1",
  "operationId": "world.create",
  "targetSide": "client",
  "input": {
    "name": "Flat Build World",
    "gameMode": "creative",
    "allowCheats": true,
    "worldType": "flat",
    "difficulty": "peaceful",
    "bonusChest": false,
    "generateStructures": false
  }
}
```

进入一个已有本地世界：

```json
{
  "requestId": "world-join-1",
  "operationId": "world.join",
  "targetSide": "client",
  "input": {
    "id": "Flat Build World"
  }
}
```

## 返回结构

`world.list` 返回：

- `worlds[].id`
- `worlds[].name`
- `worlds[].lastPlayed`
- `worlds[].gameMode`
- `worlds[].hardcore`
- `worlds[].cheatsKnown`

`world.create` 返回：

- `worldId`
- `worldName`
- `created`
- `joined`

`world.join` 返回：

- `worldId`
- `worldName`
- `joined`

## World Id 语义

- `worldId` 是本地存档目录 id，不只是显示名称
- `world.list` 会返回可复用的稳定 id
- `world.create` 现在会返回“创建并成功进入后当前真实打开世界”的 id
- `world.create` 成功后，后续应优先复用返回的 `worldId` 调用 `world.join`

## 实用说明

- `world.create` 默认 `joinAfterCreate=true`
- `world.create` 当前要求 `joinAfterCreate=true`
- 支持的 `worldType`：`default|flat|large_biomes|amplified`
- 支持的 `difficulty`：`peaceful|easy|normal|hard`
- `world.create` 支持 `bonusChest` 和 `generateStructures`
- `world.join` 接受 `id` 或 `name`，但 `id` 更稳定
- 如果多个存档共享同一个显示名，`world.join` 会返回 `world_name_ambiguous`

## 失败模型

- `world_not_found`：请求的已有存档不存在
- `world_name_ambiguous`：多个存档匹配同一个显示名
- `world_create_failed`：创建流程失败，或者游戏没有真正进入目标世界
- `world_join_failed`：进入流程失败，或者游戏没有真正进入目标世界
- `world_action_unavailable`：客户端运行时或相关 API 不可用
- `world_already_open`：请求的世界已经处于打开状态

如果游戏已经进入新世界，但 `world.create` 仍然返回 `world_not_found`，那应该被视为结果回报错误，而不是用户输入错误。
