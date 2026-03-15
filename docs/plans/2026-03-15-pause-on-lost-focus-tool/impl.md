# 2026-03-15 Pause On Lost Focus Tool Impl

## Summary

本轮新增了一个只面向 `client runtime` 的专用工具：

- `moddev.pause_on_lost_focus`

它支持两种模式：

- 不传 `enabled` 时查询当前值
- 传入 `enabled=true|false` 时设置该值并持久化保存到客户端 options

## Implementation Notes

- 新增 `PauseOnLostFocusService` 和 `LiveClientPauseOnLostFocusService`
- 实现直接读写 `Minecraft.getInstance().options.pauseOnLostFocus`
- 更新后调用 `options.save()`，保证设置会落到 `options.txt`
- 新增 `PauseOnLostFocusToolProvider`
- provider 只注册到 `ClientRuntimeBootstrap`

## Verification

### Red Phase Evidence

Focused tests initially failed with missing symbols, including:

- `cannot find symbol class PauseOnLostFocusService`
- `cannot find symbol class PauseOnLostFocusToolProvider`

This confirmed the new tool did not exist before the implementation.

### Focused Mod Tests

Run:

```powershell
$env:GRADLE_USER_HOME='C:\Projects\ModDevMCP\.gradle-user-home'; .\gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.PauseOnLostFocusToolProviderTest --tests dev.vfyjxf.mcp.runtime.tool.BuiltinProviderRegistrationTest --no-daemon --rerun-tasks
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
