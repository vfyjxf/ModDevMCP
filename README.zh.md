# ModDevMCP

ModDevMCP 正在迁移到面向 Minecraft NeoForge 调试场景的 skill-first 服务模型。

在分支 `feat/http-skill-first-architecture` 中，已批准的迁移方向是运行在 `Mod` 内部、仅绑定 loopback 的本地 HTTP 服务。Agent 通过 skills 发现能力，并通过 HTTP requests 执行 operations。

## 产品边界

- 面向最终用户的运行时产品：`:Mod`
- 仓库中仍保留的迁移期内部模块：`:Server`、`:Plugin`
- 对外流程改为 service-first，不再是 host-first

## 核心术语

- service
- skills
- categories
- operations
- requests
- status

## 入口技能与状态

- 必选入口技能：`moddev-entry`
- 服务状态接口：`/api/v1/status`
- 入口 markdown 说明 discovery、request 格式与 `targetSide` 路由规则

## 导出技能

服务会把技能投影为本地导出技能文件；导出内容不是源码真相。

常见导出结构：

- `skills/moddev-entry.md`
- `skills/<skillId>.md`
- `categories/<categoryId>.md`
- `manifest.json`

## 请求接口

- 元数据发现：`GET /api/v1/categories`、`GET /api/v1/skills`、`GET /api/v1/operations`
- 读取技能 markdown：`GET /api/v1/skills/{skillId}/markdown`
- 执行操作：`POST /api/v1/requests`
- 刷新导出技能：`POST /api/v1/skills/export`

## 最小探活

```powershell
curl http://127.0.0.1:47812/api/v1/status
```

当 `serviceReady=true` 时，先阅读 `moddev-entry`，再进入分类技能或操作技能。

## 迁移说明

- 本 README 仅用于锁定迁移期间的产品边界与术语。
- 旧的 host-first 指南尚未迁移到 service-first 流程，因此这里暂不链接。
- 英文版本：`README.md`
