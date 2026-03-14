# 2026-03-14 Plugin Provider Defaults Impl

Updated: 2026-03-15 00:05 CST

## Summary

本轮重构目标：

- 将 `modDevMcp` 公开 DSL 缩减到真正需要的字段：`enabled`、`runs`、`agentVersion`、`agentJarPath`、`requireEnhancedHotswap`
- 把 `projectRoot`、`compileTask`、`classOutputDir`、`mcpRuntimeClasspath`、host/port、main class、输出目录等默认值收回插件内部，通过 Gradle provider / task / layout API 推导
- 移除 `ModDevClientRunFlags`，不再让 `Mod/build.gradle` 保留额外的 MCP run flags 脚本逻辑
- 让 `TestMod` 和用户文档默认不再需要显式写 `modDevMcp {}`
- 把用户文档统一到“生成 client 配置 + 正常跑 `runClient` + agent 先查 `moddev.status`”这一条主流程

## Real Verification

实际执行：

- `./gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest.extensionKeepsOnlyMinimalPublicDslSurface" --no-daemon`
- `./gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
- `rg -n "ModDevClientRunFlags" .`
- `./TestMod/gradlew.bat -p . createMcpClientFiles --no-daemon`

## Result

- RED：
  - `./gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest.extensionKeepsOnlyMinimalPublicDslSurface" --no-daemon`
  - 结果：FAIL，`extensionKeepsOnlyMinimalPublicDslSurface()` 断言失败，证明公开 DSL 还没有收缩。
- 中途真实修正：
  - 第一次重构后，`./gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon` 编译失败。
  - 失败点：`Configuration`/`Provider<Directory>` 类型签名不匹配，以及插件在 apply 时过早强依赖 `compileJava` 任务，导致 `UnknownTaskException`。
  - 修正方式：把默认运行模型改成惰性 provider；`compileJava` 存在时读取真实任务，否则回退到标准默认值。
- GREEN：
  - `./gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`：`BUILD SUCCESSFUL`
  - `./TestMod/gradlew.bat -p . createMcpClientFiles --no-daemon`：`BUILD SUCCESSFUL`
- 文本验证：
  - `rg -n "ModDevClientRunFlags" .`：结果只剩历史计划文档引用，生产代码和测试代码里已无残留。
  - `TestMod/build.gradle` 已精简为只保留 mod 依赖，不再显式配置 `modDevMcp {}`。
  - `README.md`、`README.zh.md`、`docs/guides/2026-03-11-simple-agent-install-guide.md`、`docs/guides/2026-03-11-simple-agent-install-guide.zh.md` 已更新为“默认无需写 `modDevMcp {}`，只有在覆盖默认值时才写”。
  - `docs/guides/2026-03-11-game-mcp-guide*.md`、`2026-03-11-testmod-runclient-guide*.md`、`2026-03-11-agent-preflight-checklist*.md`、`2026-03-11-agent-prompt-templates*.md`、`2026-03-11-codex-screenshot-demo-guide*.md`、`2026-03-11-live-screen-tool-guide*.md`、`2026-03-12-playwright-style-ui-automation-guide*.md` 已同步到当前默认流程：
    1. 普通消费者工程默认无需写 `modDevMcp {}`
    2. `createMcpClientFiles` 是主要手工安装入口
    3. NeoForge `runClient` 会自动保持生成文件同步
    4. MCP client 使用生成配置启动 host，agent 先查 `moddev.status`
  - 代码侧做了轻量收口：`ModDevMcpPlugin` 内部注册流程提取为独立方法，常量命名统一；`CreateMcpClientFilesTask` 的空 classpath 报错文案已去掉旧 DSL 残留。
- 环境说明：
  - `apply_patch` 在更新上述计划记录时再次触发 Windows sandbox refresh 失败；这是工具环境问题，不是文档内容冲突。对应文件最终通过最小 PowerShell 文本写回完成。
