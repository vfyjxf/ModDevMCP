# 2026-03-15 Minecraft Command Support Impl

## Summary

本轮新增了 3 个 side-aware 命令工具：

- `moddev.command_list`
- `moddev.command_suggest`
- `moddev.command_execute`

实现方式延续现有多 runtime gateway：

- `client` runtime 内同时支持：
  - `ClientCommandHandler` 提供的 NeoForge 客户端命令
  - `MinecraftServer#getCommands()` 提供的 integrated server 命令
- `server` runtime 通过 `MinecraftServer#getCommands()` 提供 dedicated server 命令能力
- 对 agent 暴露为聚合后的 `common` tools
- 运行时选择继续沿用 gateway 既有动态 tool 路由，命令上下文通过 `commandSide` 选择

## Implementation Notes

### Runtime Model

- 新增 `runtime.command` 包，承载 list / suggest / execute 的 DTO 和 service 接口
- `CommandToolProvider` 改为一个 provider 内部持有两套 service：
  - `clientCommands`
  - `serverCommands`
- 新增 `BrigadierCommandSupport`，统一处理：
  - leading slash normalization
  - root command 枚举和过滤
  - Brigadier completion suggestions
  - 结构化执行结果与消息采集

### Client

- `LiveClientCommandService` 使用 `Minecraft` 主线程执行
- `LiveServerCommandService` 也会在 client runtime 中挂入 provider，用于单机 integrated server command
- 命令工具输入 schema 改为只暴露 `commandSide`，避免和 gateway 的 `targetSide` runtime 路由语义冲突
- 为避免单测环境在 provider 注册阶段触发 `ClientCommandHandler` 类解析，实际入口改为反射获取

### Server

- `LiveServerCommandService` 使用 server 线程执行
- source 提升到 `LEVEL_OWNERS`
- `command_execute` 在 runtime descriptor 上标记为 mutating
- 移除了 common 层对 command tool 的重复注册，避免单个 registry 中出现两份同名 `moddev.command_*`

## Verification

### Focused Mod Tests

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*CommandToolProviderTest" --tests "*BuiltinProviderRegistrationTest" --no-daemon --rerun-tasks
```

Result:

- `BUILD SUCCESSFUL`

### Focused Mod Verification

Run:

```powershell
set GRADLE_USER_HOME=C:\Projects\ModDevMCP\.gradle-user-home&& gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.CommandToolProviderTest --no-daemon --rerun-tasks
```

Result:

- `BUILD SUCCESSFUL`

## Notes

- `Mod:compileJava` 会打印既有的 unchecked warning，来源是 `BrigadierCommandSupport` 对 Brigadier / client reflection 的必要桥接，不影响构建结果。
- 本轮验证覆盖了 provider 注册、gateway 聚合路由和全仓单测；尚未单独补跑真实 `runClient` / dedicated server 命令调用会话。
