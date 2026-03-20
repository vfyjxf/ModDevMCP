# ModDevMCP

ModDevMCP 现在使用面向 Minecraft NeoForge 调试场景的 skill-first 服务模型。

在分支 `feat/http-skill-first-architecture` 中，当前生效的产品是运行在 `Mod` 内部、仅绑定 loopback 的本地 HTTP 服务。Agent 通过 skills 发现能力，并通过 HTTP requests 执行 operations。

## 产品边界

- 面向最终用户的运行时产品：`:Mod`
- 旧的 `Server` 与 `Plugin` 模块已经移出当前活动构建。
- 对外流程是 service-first。

## 核心术语

- service
- skills
- categories
- operations
- requests
- status

## 入口技能与状态

- 必选入口技能：`moddev-usage`
- 服务状态接口：`/api/v1/status`
- 入口 markdown 说明 discovery、request 格式与 `targetSide` 路由规则

## 发现流程

- 先做默认探测：`http://127.0.0.1:47812/api/v1/status`
- 如果不可用，走项目级回退：`<gradleProject>/build/moddevmcp/game-instances.json`
- 用 `GET /api/v1/status` 探测文件里的候选 `baseUrl`
- 当 client 和 server 同时运行时，client 和 server 使用独立端口

## 导出技能

服务会把技能投影为本地导出技能文件；导出内容不是源码真相。

常见导出结构：

- `skills/moddev-usage.md`
- `skills/<skillId>.md`
- `categories/<categoryId>.md`
- `manifest.json`

## 请求接口

- 元数据发现：`GET /api/v1/categories`、`GET /api/v1/skills`、`GET /api/v1/operations`
- 读取技能 markdown：`GET /api/v1/skills/{skillId}/markdown`
- 执行操作：`POST /api/v1/requests`
- 刷新导出技能：`POST /api/v1/skills/export`

## 本地世界 Operation

- 对外公开的本地世界 operation 是 `world.list`、`world.create`、`world.join`
- 即使 integrated server 已连上，它们仍然属于 client 侧 operation
- `world.create` 成功后，后续进入应优先复用返回的 `worldId`
- `worldId` 是本地存档目录 id，不只是显示名称

## 最小探活

```powershell
curl http://127.0.0.1:47812/api/v1/status
```

当 `serviceReady=true` 时，先阅读 `moddev-usage`，再进入分类技能或操作技能。

## 说明

- 本 README 用来锁定当前产品边界与服务术语。
- tools/ 下 legacy JSON-RPC 辅助脚本已经移出当前活跃流程。
- 使用方只需要引入发布后的 `dev.vfyjxf:moddevmcp` 依赖。
- 英文版本：`README.md`


