# 本地世界 Operation 指南

日期：2026-03-15
更新：2026-03-20

## 目的

使用 `world.list` / `world.create` / `world.join` 管理本地单人世界。

## Operation

- `world.list`
- `world.create`
- `world.join`

## 说明

- 本地世界操作始终走客户端侧
- `worldId` 是存档目录 id，不是展示名称

## 示例

列出本地世界：

```json
{
  "operationId": "world.list",
  "targetSide": "client",
  "input": {}
}
```

创建新世界：

```json
{
  "operationId": "world.create",
  "targetSide": "client",
  "input": {
    "name": "Test World",
    "gameMode": "survival",
    "allowCheats": false
  }
}
```

加入已有世界：

```json
{
  "operationId": "world.join",
  "targetSide": "client",
  "input": {
    "id": "MyWorld"
  }
}
```
