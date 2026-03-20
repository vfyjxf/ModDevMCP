# 2026-03-11 简明 Agent 安装指南

Date: 2026-03-11 17:30 CST
Updated: 2026-03-18 19:30 CST

## 用途

- 说明内置本地 service 的最简消费者接入方式
- 让 agent 启动流程与导出 skills、request API 保持一致
- 避免手写外部桥接或独立 server 启动命令

## 消费者工程配置

在你自己的 NeoForge 工程里添加已发布的 mod 依赖：

```groovy
dependencies {
    implementation("dev.vfyjxf:moddevmcp:<version>") {
        transitive = false
    }
}
```

这个 mod 会在游戏进程内部直接暴露 loopback HTTP service。

## 启动顺序

1. 启动你平时使用的游戏运行任务，例如 `runClient`
2. 等待 mod 完成加载
3. 先做默认探测 `GET http://127.0.0.1:47812/api/v1/status`
4. 若不可用，走项目级回退 `<gradleProject>/build/moddevmcp/game-instances.json`
5. 对候选地址执行 `GET /api/v1/status`，选中可用 `baseUrl`
6. 阅读 `moddev-usage`
7. 再继续调用 `POST /api/v1/requests`

当双端同时活跃时，client 和 server 使用独立端口。

## 导出技能

默认会把本地 skill 树导出到：

- `~/.moddev/skills/manifest.json`
- `~/.moddev/skills/skills/moddev-usage.md`
- `~/.moddev/skills/skills/<skillId>.md`
- `~/.moddev/skills/categories/<categoryId>.md`

如果本地文件已经存在，agent 应优先先读导出的入口 skill。

## 最小验证

先按同一流程解析 `baseUrl`：

1. `GET http://127.0.0.1:47812/api/v1/status`
2. 若不可用，读取 `<gradleProject>/build/moddevmcp/game-instances.json`
3. 对候选地址执行 `GET /api/v1/status`，选中可用 `baseUrl`

然后执行：

```powershell
curl <baseUrl>/api/v1/status
curl <baseUrl>/api/v1/skills/moddev-usage/markdown
```

```powershell
curl -X POST <baseUrl>/api/v1/requests `
  -H "Content-Type: application/json" `
  -d '{"requestId":"check-1","operationId":"status.get","input":{}}'
```

## 相关文档

- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-testmod-runclient-guide.md`
