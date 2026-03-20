# 2026-03-14 Remove Dev UI Capture Flag Impl

## Summary

本次清理删除了 `moddevmcp.devUiCapture` 相关的自动挂载链路：

- 删除 `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/DevUiCaptureFlags.java`
- 删除 `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/ClientDevUiCaptureVerifier.java`
- 删除 `Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/ClientDevUiCaptureVerifierTest.java`
- 更新 `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`，移除 verifier 挂载
- 更新 `buildSrc/src/main/java/dev/vfyjxf/gradle/ModDevClientRunFlags.java`，不再解析 `moddevmcp.devUiCapture`
- 更新 `buildSrc/src/test/java/dev/vfyjxf/gradle/ModDevClientRunFlagsTest.java`，改为验证只转发 MCP host/port
- 更新 `TestMod/build.gradle`，移除 `moddevmcp.devUiCapture` 透传
- 清理历史计划文档里对该属性的启动示例

`DevUiCaptureVerificationRunner` 保留，当前只是不再通过客户端启动自动触发。

## Verification

### Red

先修改 `buildSrc` 测试，让它要求忽略 `moddevmcp.devUiCapture`：

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :buildSrc:test --tests "*ModDevClientRunFlagsTest" --no-daemon
```

Result:

- `3 tests completed, 3 failed`
- 三个失败都来自 `ModDevClientRunFlagsTest`
- 真实原因是 helper 还在转发 `moddevmcp.devUiCapture`

### Green

去掉 helper 中的属性解析后重新验证：

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :buildSrc:test --tests "*ModDevClientRunFlagsTest" --no-daemon
```

Result:

- `BUILD SUCCESSFUL`

### Runtime Verification

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --no-daemon
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

- `TestMod/build.gradle` 在 `apply_patch` 上连续触发了 Windows sandbox refresh 错误，不是代码错误，也不是文件只读。
- 因为补丁工具对该文件失效，这一处改动改用最小的 shell 重写完成，其余代码删除仍然使用了 `apply_patch`。
