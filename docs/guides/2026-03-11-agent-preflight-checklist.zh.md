# 2026-03-11 Agent 预检查清单

Date: 2026-03-11 17:30 CST
Updated: 2026-03-15 00:05 CST

## 用途

- 强制采用简单的就绪检查流程
- 防止 agent 猜测游戏是否已经可用
- 要求在使用游戏工具前做明确的实时检查

## 推荐流程

1. 先确认生成的 ModDev服务配置已经安装到 agent client
2. 用你平时的运行任务启动 Minecraft
3. 让 agent client 按生成配置建立连接
4. 等待游戏完成加载
5. 调用 `GET /api/v1/status`
6. 只有在 `gameReady=true` 时继续
7. 调用 `status.live_screen (via POST /api/v1/requests)`
8. 只有在该调用成功时继续

## Agent 硬规则

- 如果 服务连接失败，立即停止
- 如果 `GET /api/v1/status` 返回 `gameConnected=false`，停止或等待
- 如果 `status.live_screen (via POST /api/v1/requests)` 失败，停止
- 在确认就绪之前，不要调用 UI、input、inventory、capture 或 event 工具
- 不要伪造截图、UI 树或交互结果
- 不要根据旧文件或之前的运行结果推断当前就绪状态

## 简短预检查提示词

```text
只有在 service host 可用且 Minecraft 已经加载完成后，才使用 ModDevMCP。

普通消费者接入时，不需要为了达到这个状态而额外写 an extra Gradle override block。

预检查规则：
1. 在调用 `GET /api/v1/status` 之前，不要使用游戏工具。
2. 只有在 `GET /api/v1/status` 返回 `gameReady=true` 时才继续。
3. 然后调用 `status.live_screen (via POST /api/v1/requests)`。
4. 只有在该调用成功时才继续。
5. 如果 服务连接失败，或任一检查失败，就停止并告诉用户游戏尚未就绪。
6. 不要根据旧文件、旧截图或更早的运行结果声称游戏已经就绪。
```
