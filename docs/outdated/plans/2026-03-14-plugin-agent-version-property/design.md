# 2026-03-14 Plugin Agent Version Property Design

## Context

当前 `modDevMcp` 扩展把 agent 依赖配置暴露为完整的 `agentCoordinates` 字符串。这会把 group/artifact 细节暴露给消费者，并让用户在正常场景下重复配置本来应该由插件固定的部分。

同时，README 和示例工程里当前仍使用：

- `agentCoordinates = "dev.vfyjxf:moddevmcp-agent:<version>"`
- `implementation("dev.vfyjxf:moddevmcp-mod:<version>")`

新的要求是：

- agent 坐标与插件绑定，不再让用户配置完整坐标
- 改成一个版本属性
- mod 依赖的发布坐标改成 `dev.vfyjxf:moddevmcp`

## Decision

采用“固定坐标 + 可覆盖版本”的方案：

- 删除 `ModDevMcpExtension.agentCoordinates`
- 新增 `ModDevMcpExtension.agentVersion`
- 插件内部固定 agent 坐标前缀为 `dev.vfyjxf:moddevmcp-agent:`
- 默认 `agentVersion` 由插件源码内置常量提供；只有显式覆盖时才读取用户配置
- `agentJarPath` 继续保留，并优先于 Maven 解析，作为显式本地覆盖
- 用户文档和示例工程统一改为：
  - `agentVersion = "<version>"`
  - `implementation("dev.vfyjxf:moddevmcp:<version>")`

## Scope

本轮改动范围：

- `Plugin` 扩展和注入逻辑
- `Plugin` 相关测试
- `TestMod/build.gradle`
- `README.md` / `README.zh.md`
- 相关用户 guide 示例

## Validation

需要做的真实验证：

1. 先写失败测试覆盖 `agentVersion` 新模型
2. 跑插件定向测试，确认 RED
3. 实现后重跑插件定向测试，确认 GREEN
4. 跑至少一个 `TestMod` 相关 Gradle 任务，确认新 DSL 未破坏基本集成
5. 记录真实结果；如果依赖下载失败，要明确标注为环境问题
