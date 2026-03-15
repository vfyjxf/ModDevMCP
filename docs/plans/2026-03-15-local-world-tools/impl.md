# 2026-03-15 Local World Tools Impl

## Summary

本轮新增了 3 个只面向 `client runtime` 的本地世界工具：

- `moddev.world_list`
- `moddev.world_create`
- `moddev.world_join`

实现方式不是纯 UI 点击，而是：

- 世界列表通过 `LevelStorageSource.findLevelCandidates/loadLevelSummaries`
- 进入世界通过 `Minecraft.createWorldOpenFlows().openWorld(...)`
- 创建世界通过 `CreateWorldScreen.openFresh(...)` 初始化默认创建流，再设置 `WorldCreationUiState` 并触发创建

这样可以保持真实客户端窗口状态与实际进入的世界一致。

## Implementation Notes

### Runtime Model

- 新增 `runtime.world` 包，承载 request/result DTO、service 接口和异常
- 新增 `WorldToolProvider`
- provider 只注册到 `ClientRuntimeBootstrap`
- tool side 统一为 `client`

### World List

- 读取本地 save folder 候选项
- 加载 `LevelSummary`
- 返回：
  - `id`
  - `name`
  - `lastPlayed`
  - `gameMode`
  - `hardcore`
  - `cheatsKnown`

### World Join

- 优先按 `id` 解析
- 若只给 `name`：
  - 无匹配返回 `world_not_found`
  - 多匹配返回 `world_name_ambiguous`
- 调用 `WorldOpenFlows.openWorld`
- 通过轮询 integrated server 当前世界名判断加载完成

### World Create

- 使用 `CreateWorldScreen.openFresh(...)`
- 配置：
  - `name`
  - `gameMode`
  - `allowCheats`
  - `seed`
  - `worldType`
  - `difficulty`
  - `bonusChest`
  - `generateStructures`
- 通过反射触发 `CreateWorldScreen.onCreate()`
- 当前版本要求 `joinAfterCreate=true`
- 当前支持的 world preset:
  - `default`
  - `flat`
  - `large_biomes`
  - `amplified`

## Verification

### Red Phase Evidence

IDE build on the new tests initially failed with missing symbols, including:

- `package dev.vfyjxf.mcp.runtime.world does not exist`
- `cannot find symbol class WorldToolProvider`

This confirmed the failure came from the missing world tool implementation.

### Focused Mod Tests

Run:

```powershell
$env:GRADLE_USER_HOME='C:\Projects\ModDevMCP\.gradle-user-home'; .\gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.WorldToolProviderTest --no-daemon --rerun-tasks
```

Result:

- `BUILD SUCCESSFUL`

### Compile Verification

Run:

```powershell
$env:GRADLE_USER_HOME='C:\Projects\ModDevMCP\.gradle-user-home'; .\gradlew.bat :Mod:compileJava --no-daemon
```

Result:

- `BUILD SUCCESSFUL`

## Notes

- `world_create` currently rejects `joinAfterCreate=false` with `world_action_unavailable`
- 本轮没有补真实 `runClient` MCP 会话下的 live create/join 演练，因此剩余风险集中在实际 UI/客户端线程时序
- `flatPreset` 这一轮没有开放；当前只支持切换到 flat preset 本身
