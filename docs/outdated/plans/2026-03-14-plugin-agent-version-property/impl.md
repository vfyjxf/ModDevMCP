# 2026-03-14 Plugin Agent Version Property Impl

## Summary

本轮目标：

- 将 `modDevMcp.agentCoordinates` 改为 `modDevMcp.agentVersion`
- 让插件内部固定解析 `dev.vfyjxf:moddevmcp-agent:<agentVersion>`
- 将用户示例中的 mod 依赖改为 `dev.vfyjxf:moddevmcp`
- 为 `Agent` 保留独立 `agent_version`
- 后续将插件默认 `agentVersion` 简化为源码内置常量 `0.1.1`

## Real Verification

实际执行：

- `./gradlew.bat :Plugin:test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
- `./gradlew.bat :Mod:test --tests "*HotswapServiceIntegrationTest.reloadReportsAgentClassloaderDiagnostics" --no-daemon`
- `./gradlew.bat :Agent:publishToMavenLocal --no-daemon`
- `./TestMod/gradlew.bat -p ./TestMod createMcpClientFiles --no-daemon`
- `rg -n 'agentVersion = "<version>"' README.md README.zh.md docs/guides -g '*.md'`

## Result

- RED：
  - 早先执行 `./gradlew.bat :Plugin:compileTestJava --no-daemon`，因 `getAgentVersion()` 不存在而失败，证明新增测试先于实现生效。
  - 本轮新增 `systemAgentVersion` 断言后，执行 `./gradlew.bat :Mod:test --tests "*HotswapServiceIntegrationTest.reloadReportsAgentClassloaderDiagnostics" --no-daemon`，测试失败，随后补实现。
- GREEN：
  - `./gradlew.bat :Plugin:test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`：`BUILD SUCCESSFUL`
  - `./gradlew.bat :Mod:test --tests "*HotswapServiceIntegrationTest.reloadReportsAgentClassloaderDiagnostics" --no-daemon`：`BUILD SUCCESSFUL`
  - `./gradlew.bat :Agent:publishToMavenLocal --no-daemon`：`BUILD SUCCESSFUL`
  - `./TestMod/gradlew.bat -p ./TestMod createMcpClientFiles --no-daemon`：`BUILD SUCCESSFUL`
- 中途真实问题：
  - `Agent:processResources` 一度因 configuration cache 不允许闭包执行期引用 `project` 而失败，已通过提前求值修复；这是构建脚本问题，不是网络问题。
  - `TestMod createMcpClientFiles` 一度因本地缺少 `dev.vfyjxf:moddevmcp-agent:0.1.1` 而失败；在执行 `:Agent:publishToMavenLocal` 后恢复正常。这是本地 Maven 工件缺失，不是 TLS / 仓库握手问题。
  - 删除插件旧资源文件时，`apply_patch` 两次被 Windows sandbox refresh 问题拦截，最终用最小 `Remove-Item` 删除，不影响代码结果。
- 文档结果：
  - `README.md`、`README.zh.md`、`docs/guides/2026-03-11-simple-agent-install-guide.md`、`docs/guides/2026-03-11-simple-agent-install-guide.zh.md` 已更新为“插件内置默认 `agentVersion`，仅在需要覆盖时显式设置”。
