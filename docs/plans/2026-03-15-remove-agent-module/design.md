# 2026-03-15 Remove Agent Module Design

Date: 2026-03-15 00:25 CST

## Goal

移除独立 `Agent` 模块与插件里的 agent 注入逻辑，把后续仍需要的 instrumentation 获取能力直接并入 `Mod`，并改为依赖 `net.lenni0451:Reflect:1.6.2` 的 `Agents` 能力。

## Design

- 删除独立 `Agent` 发布物与构建接线，不再保留 `moddevmcp-agent`。
- `Mod` 直接声明 `implementation("net.lenni0451:Reflect:1.6.2")`。
- `HotswapService` 不再反射 `dev.vfyjxf.mcp.agent.HotswapAgent`，直接调用 `Reflect` 的 `Agents` API 获取 `Instrumentation`。
- Gradle 插件不再注入 `-javaagent`，同时移除 `modDevMcp.agentVersion`、`modDevMcp.agentJarPath`。
- `Mod` 的开发运行与测试都不再依赖独立 agent jar。

## Impact

- hotswap 的 instrumentation 来源从“外部 agent 模块”切换成“Mod 内直接使用 Reflect”。
- 插件的公开 DSL 进一步收缩。
- 用户文档需要删除 agent 相关配置说明。

## Validation

- 插件测试应验证：不再注入 `-javaagent`，公开 DSL 中不再暴露 agent 字段。
- `Mod` hotswap 测试应验证：instrumentation 不可用时返回新的真实错误，不再提到 `HotswapAgent`。
- `TestMod createMcpClientFiles` 应继续成功。
