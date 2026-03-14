# 2026-03-14 Game Close Tool Impl

## Summary

本轮把 `moddev.game_close` 从“纯 client tool”重构成了“common tool + 多 runtime gateway + client/server 双端实现”：

### Server

- `RuntimeRegistry` 从单 `activeSession` 改成多 runtime 存储
- 动态 tool 列表会按 tool name 聚合，避免 `client` / `server` 同名 tool 重复暴露
- 新增 side-aware 路由：
  - `targetSide=client|server` 精确路由
  - 单 side 在线时自动选中
  - 双 side 都在线且未指定时返回 `ambiguous_runtime_side: specify targetSide`
- `moddev.status` 现在同时报告：
  - `clientConnected`
  - `serverConnected`
  - `connectedRuntimes`
- `RuntimeHost` 也从单连接改成按 `runtimeId` 维护多个 runtime connection

### Mod

- 新增 `GameCloser` 抽象
- `LiveClientGameCloser` 负责优雅关闭客户端
- `LiveServerGameCloser` 负责优雅停止 dedicated server
- `GameToolProvider` 改为 common tool，schema 增加可选 `targetSide`
- 新增 `ServerRuntimeBootstrap`
- `ServerEntrypoint` 现在会像 client 一样自动连到 gateway
- `HostGameClient` 会按 runtime side 发送正确的 `supportedScopes`

## API Query

本轮对双端关闭 API 都做了本地查询，而不是凭记忆硬写。

### Client

Run:

```powershell
javap -classpath Mod\build\moddev\artifacts\neoforge-21.1.219-merged.jar net.minecraft.client.Minecraft
```

关键结果：

- 存在 `public void close();`
- 存在 `public void stop();`

继续查看字节码后，确认 `stop()` 是“请求退出”，`close()` 更偏资源清理，因此 client 侧继续使用 `Minecraft.stop()`。

### Server

Run:

```powershell
javap -classpath Mod\build\moddev\artifacts\neoforge-21.1.219-merged.jar net.minecraft.server.MinecraftServer
javap -classpath .gradle-user\caches\modules-2\files-2.1\net.neoforged\neoforge\21.1.219\300c6ecf584eab19b4dca5e69cc2ee68d0d21f1f\neoforge-21.1.219-universal.jar net.neoforged.neoforge.server.ServerLifecycleHooks
```

关键结果：

- `MinecraftServer` 存在 `stopServer()`、`halt(boolean)`、`isRunning()`
- `ServerLifecycleHooks` 存在 `getCurrentServer()`

再看 `halt(boolean)` 字节码，确认它会把 `running` 置为 `false`。因此 server 侧实现选 `ServerLifecycleHooks.getCurrentServer()` + `server.execute(() -> server.halt(false))`，表达“请求优雅停服”。

## Verification

### Server Red

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Server:test --tests "*RuntimeRegistryTest" --tests "*HostStatusToolProviderTest" --tests "*McpProtocolRuntimeDispatchTest" --no-daemon --rerun-tasks
```

Result:

- `BUILD FAILED`
- 真实失败点是 `RuntimeRegistry` 缺失 `listSessions()` / `findSession(...)`
- 说明多 runtime 模型在改动前确实不存在

### Server Green

实现 multi-runtime registry / routing 后重新执行：

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Server:test --tests "*RuntimeRegistryTest" --tests "*HostStatusToolProviderTest" --tests "*McpProtocolRuntimeDispatchTest" --no-daemon --rerun-tasks
```

Result:

- `BUILD SUCCESSFUL`

### Mod Red

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*BuiltinProviderRegistrationTest" --tests "*GameToolProviderTest" --tests "*HostGameClientTest" --no-daemon --rerun-tasks
```

Result:

- `BUILD FAILED`
- 真实失败点包括：
  - 缺少 `GameCloser`
  - `GameToolProvider` 仍绑定旧的 `ClientGameCloser`
  - dedicated server runtime bootstrap / hello 逻辑尚未实现

### Mod Green

补完 common `game_close`、server runtime bootstrap、server hello scope 后重新执行：

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*BuiltinProviderRegistrationTest" --tests "*GameToolProviderTest" --tests "*HostGameClientTest" --no-daemon --rerun-tasks
```

Result:

- `BUILD SUCCESSFUL`

### Full Verification

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat test --no-daemon
```

Result:

- `BUILD SUCCESSFUL`

## Notes

- 本轮没有跑实机 `runClient` / `runServer` 关闭验证；当前证据是双端单测 + gateway 路由单测 + 全仓测试通过。
- `apply_patch` 在这个 Windows sandbox 会话里持续报 `windows sandbox: setup refresh failed`，不是 Java/Gradle 代码错误。本轮代码和文档仍然通过最小 shell 写入完成。

## Live Verification

本轮还补做了真实双端启动与关闭验证。

### Startup

按顺序启动：

```powershell
.\gradlew.bat :Server:runServer --no-daemon
.\gradlew.bat :Mod:runServer --no-daemon
.\gradlew.bat :Mod:runClient --no-daemon
```

backend 日志真实出现：

- `moddev backend ready runtime=127.0.0.1:47653 mcp=127.0.0.1:47654`
- `runtime connected: server-runtime`
- `runtime connected: client-runtime`

### Live MCP Status

通过 TCP MCP 端口 `127.0.0.1:47654` 实际调用 `moddev.status`，真实返回：

- `clientConnected=true`
- `serverConnected=true`
- `connectedRuntimes` 同时包含 `server-runtime` 与 `client-runtime`

### Live Server Close

实际调用：

```json
{"name":"moddev.game_close","arguments":{"targetSide":"server"}}
```

真实返回：

- `accepted=true`
- `runtimeId=server-runtime`
- `runtimeSide=server`

随后再次查询 `moddev.status`，真实变为：

- `serverConnected=false`
- `clientConnected=true`

### Live Client Close

实际调用：

```json
{"name":"moddev.game_close","arguments":{"targetSide":"client"}}
```

真实返回：

- `accepted=true`
- `runtimeId=client-runtime`
- `runtimeSide=client`

随后再次查询 `moddev.status`，真实变为：

- `gameConnected=false`
- `clientConnected=false`
- `serverConnected=false`
- `connectedRuntimes=[]`
