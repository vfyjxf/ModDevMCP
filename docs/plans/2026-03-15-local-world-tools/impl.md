# 2026-03-15 Local World Operations Impl

## Summary

本轮对外新增了 3 个本地世界 operation：

- `world.list`
- `world.create`
- `world.join`

mod 内部由 3 个只面向 `client runtime` 的 provider tool 承接：

- `moddev.world_list`
- `moddev.world_create`
- `moddev.world_join`

实现方式不是纯 UI 点击，而是：

- 世界列表通过 `LevelStorageSource.findLevelCandidates/loadLevelSummaries`
- 进入世界通过 `Minecraft.createWorldOpenFlows().openWorld(...)`
- 创建世界通过 `CreateWorldScreen.openFresh(...)` 初始化默认创建流，再设置 `WorldCreationUiState` 并触发创建
- create 成功后的返回结果直接来自当前 integrated server 的真实世界上下文，而不是依赖一次新的 summary 扫描

这样可以保持真实客户端窗口状态与实际进入的世界一致。

## Implementation Notes

### Runtime Model

- 新增 `runtime.world` 包，承载 request/result DTO、service 接口和异常
- 新增 `WorldToolProvider`
- provider 只注册到 `ClientRuntimeBootstrap`
- tool side 统一为 `client`
- HTTP service 再把这些能力投影为 `world.list|create|join`

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
- 在确认游戏已经进入目标世界后，直接读取当前 integrated server：
  - `worldName` 来自当前世界数据
  - `worldId` 来自 `level.dat` 所在存档目录名
- 不再把“创建后立刻重新扫描 summary 列表能否找到新世界”当成 create 成功条件

## Verification

### Red Phase Evidence

IDE build on the new tests initially failed with missing symbols, including:

- `package dev.vfyjxf.mcp.runtime.world does not exist`
- `cannot find symbol class WorldToolProvider`

This confirmed the failure came from the missing world tool implementation.

### Focused Mod Tests

Run:

```powershell
$env:GRADLE_USER_HOME='D:\ProjectDir\AgentFarm\ModDevMCP\.gradle-user'; .\gradlew.bat :Mod:test --tests "*WorldToolProviderTest" --tests "*McpToolRegistryTest" --tests "*OperationRequestEndpointTest" --tests "*GameToolProviderTest" --tests "*CommandToolProviderTest" --no-daemon
```

Result:

- `BUILD SUCCESSFUL`

### Compile Verification

Run:

```powershell
$env:GRADLE_USER_HOME='D:\ProjectDir\AgentFarm\ModDevMCP\.gradle-user'; .\gradlew.bat :Mod:compileJava --no-daemon
```

Result:

- compile was covered during the test and publish runs
- `BUILD SUCCESSFUL`

### Real TestMod Validation

Real validation was rerun on 2026-03-18 with:

- JBR: `C:\Users\vfyjx\.jdks\jbr-21.0.10`
- real user home: `-Duser.home=C:\Users\vfyjx`
- real local Maven: `-Dmaven.repo.local=C:\Users\vfyjx\.m2\repository`
- Gradle-launched `TestMod:runClient`

Observed result:

- `world.create` returned `status=ok`
- the returned `worldId` matched the created local save-folder id
- status then reported `connectedSides=["client","server"]`
- `command.execute` on `server` successfully gave `minecraft:netherite_sword`

## Notes

- `world_create` currently rejects `joinAfterCreate=false` with `world_action_unavailable`
- 本轮已经补过真实 `runClient` 会话下的 create/join 演练，核心剩余风险主要集中在 Minecraft 上游 UI 或存档 API 变动
- `flatPreset` 这一轮没有开放；当前只支持切换到 flat preset 本身
