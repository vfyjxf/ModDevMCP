# 2026-03-14 Bilingual User Docs Design

## Context

当前 `README.md` 同时承载了用户入口、仓库结构、内部实现说明、调试命令、验证记录，职责过重，而且还混入了编码异常的中文文本。现有 `docs/guides` 也存在两个问题：

- 有些内容仍然偏内部实现视角，不够面向终端用户
- 用户需要英文版给 agent 使用，同时也要有完整中文镜像给人看

## Decision

采用“英文主文档 + 中文镜像 + README 极简入口”方案：

- `README.md` 作为英文主入口，只保留用户需要的安装、启动、就绪检查和文档索引
- `README.zh.md` 作为中文镜像
- `docs/guides/*.md` 统一整理为英文用户文档
- `docs/guides/*.zh.md` 提供一一对应的中文镜像
- `docs/plans` 继续作为内部设计/计划/实现记录，不参与双语镜像

## Scope

本轮只处理面向用户的文档：

- `README.md`
- `README.zh.md`
- `docs/guides/2026-03-11-simple-agent-install-guide.md`
- `docs/guides/2026-03-11-game-mcp-guide.md`
- `docs/guides/2026-03-11-testmod-runclient-guide.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-agent-prompt-templates.md`
- `docs/guides/2026-03-11-codex-screenshot-demo-guide.md`
- `docs/guides/2026-03-11-live-screen-tool-guide.md`
- `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- 上述 guides 的全部 `.zh.md` 镜像

## Content Rules

README 只保留：

- 项目是什么
- 用户如何生成 MCP client 配置
- 用户如何启动 host 和游戏
- 如何做最小可用检查
- 去哪里看更详细的 guide

README 移除：

- 仓库模块布局
- 内部 gateway/backend 细节
- 实现能力清单
- 验证记录
- 仓库内部调试说明

guides 的要求：

- 英文主版本可直接给 agent 使用
- 中文版本保持内容对应，不新增只给中文读者的特殊规则
- 减少内部术语，把重点放在“用户怎么做”

## Validation

需要做的真实验证：

1. 检查所有目标文档都已存在
2. 检查 README 与 guide 之间的双语链接是否可解析
3. 用 `git diff --stat` 和 `git status --short` 确认真实改动范围
