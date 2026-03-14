# 2026-03-11 TestMod RunClient 指南

Date: 2026-03-11 17:30 CST
Updated: 2026-03-15 00:05 CST

## 用途

- 用独立 NeoForge 客户端做真实验证
- 保持 `TestMod` 作为参考消费者工程
- 保持 MCP client 安装文件与游戏运行流程一致

## 消费者形态

- `TestMod` 按普通消费者工程接入
- 默认客户端流程下不需要显式写 `modDevMcp {}`
- `runClient` 依赖 `createMcpClientFiles`
- 你的 MCP client 会根据安装好的配置启动生成的 host 入口

## 启动客户端

在 `TestMod` 中执行：

```powershell
cd TestMod
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat runClient --no-daemon
```

需要时也可以手动生成 client 文件：

```powershell
.\gradlew.bat createMcpClientFiles --no-daemon
```

`runClient` 同时依赖 client 文件生成，因此 MCP 安装文件会保持同步。

## 用户会看到什么

- Minecraft 客户端启动
- mod 随游戏一起加载
- 在 MCP client 启动生成的 host 入口后，游戏会自动回连到 host
- 生成出来的 MCP client 文件与当前构建保持一致

## Agent 就绪检查

1. 调用 `moddev.status`
2. 确认 `gameConnected=true`
3. 调用 `moddev.ui_get_live_screen`

只有这三步都成功后才继续。
