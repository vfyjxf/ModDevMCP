# 2026-03-11 TestMod RunClient 指南

Date: 2026-03-11 17:30 CST
Updated: 2026-03-18 14:30 CST

## 用途

- 用独立 NeoForge 客户端做真实验证
- 保持 `TestMod` 作为参考消费者工程
- 验证本地 HTTP service 与导出 skills 在真实游戏里的行为

## 启动客户端

在 `TestMod` 中执行：

```powershell
cd TestMod
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat runClient --no-daemon
```

## 用户会看到什么

- Minecraft 客户端启动
- `ModDevMCP` 在游戏内部加载
- loopback 本地 service 可用
- 导出 skill 树写入 `~/.moddev/skills`

## 验证步骤

当游戏到达标题界面或进入世界后：

```powershell
curl http://127.0.0.1:47812/api/v1/status
curl http://127.0.0.1:47812/api/v1/skills/moddev-entry/markdown
```

可选请求探测：

```powershell
curl -X POST http://127.0.0.1:47812/api/v1/requests `
  -H "Content-Type: application/json" `
  -d '{"requestId":"probe-1","operationId":"status.get","input":{}}'
```

```powershell
curl -X POST http://127.0.0.1:47812/api/v1/requests `
  -H "Content-Type: application/json" `
  -d '{"requestId":"probe-2","operationId":"status.live_screen","input":{}}'
```

## Agent 就绪检查

1. `GET /api/v1/status`
2. 确认 `serviceReady=true`
3. 如果任务依赖真实游戏状态，再确认 `gameReady=true`
4. 阅读 `moddev-entry`
5. 再继续执行 request API 调用
