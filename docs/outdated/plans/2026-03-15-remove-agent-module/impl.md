# 2026-03-15 Remove Agent Module Impl

Updated: 2026-03-15 00:45 CST

## Summary

本轮变更完成了以下迁移：

- 删除独立 `Agent` 模块与对应构建接线
- Gradle 插件不再解析 `moddevmcp-agent`，也不再给运行任务注入 `-javaagent`
- `modDevMcp` 公开 DSL 移除了 `agentVersion`、`agentJarPath`
- `Mod` 直接依赖 `net.lenni0451:Reflect:1.6.2`
- `HotswapService` 改为直接通过 `Reflect` 的 `Agents.getInstrumentation()` 获取 `Instrumentation`

## Code Changes

- `settings.gradle` 移除了 `:Agent`
- `gradle.properties` 移除了 `agent_version`
- `Agent/build.gradle`、`Agent/src/main/java/dev/vfyjxf/mcp/agent/HotswapAgent.java`、`Agent/src/main/java/dev/vfyjxf/mcp/agent/HotswapCapabilities.java` 已删除
- `Mod/build.gradle`
  - 删除 `project(":Agent")`
  - 删除 run/test 的 `-javaagent` 配置
  - 新增 `Reflect 1.6.2` 的 `implementation` 与 `jarJar`
- `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpExtension.java`
  - 删除 `agentVersion`、`agentJarPath`
- `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpPlugin.java`
  - 删除 agent 默认值与解析逻辑
- `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/neoforge/NeoForgeRunInjector.java`
  - 删除 `-javaagent` 注入与 agent 工件解析
- `Mod/src/main/java/dev/vfyjxf/mcp/runtime/hotswap/HotswapService.java`
  - 删除 `HotswapAgent` 反射路径
  - 改为直接通过 `Agents.getInstrumentation()`
  - diagnostics 改成 `instrumentationPresent` / `instrumentationProvider` / `instrumentationError`

## Real Verification

实际执行：

- `./gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
- `./gradlew.bat :Mod:test --tests "*HotswapServiceIntegrationTest" --no-daemon`
- `./TestMod/gradlew.bat -p . createMcpClientFiles --no-daemon`
- `rg -n 'agentVersion|agentJarPath|moddevmcp-agent|HotswapAgent|:Agent' README.md README.zh.md docs/guides Plugin/src/main Mod/src/main TestMod settings.gradle gradle.properties -g "*.md" -g "*.java" -g "*.gradle"`

## Result

- RED:
  - `./gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
  - 结果：FAIL
  - 真实失败点：
    - `ModDevMcpPluginTest.extensionKeepsOnlyMinimalPublicDslSurface()` 仍能看到 agent getter
    - `NeoForgeRunInjectorTest` 仍在尝试解析 `moddevmcp-agent`
- RED:
  - `./gradlew.bat :Mod:test --tests "*HotswapServiceIntegrationTest" --no-daemon`
  - 结果：FAIL
  - 真实失败点：`HotswapService` 仍返回 `HotswapAgent` 相关 diagnostics / error
- 中途真实修正：
  - 第一次把 `Agents::getInstrumentation` 直接传给 `Supplier<Instrumentation>` 时，`:Mod:compileJava` 失败
  - 失败点：`Agents.getInstrumentation()` 抛出 `IOException`，与 `Supplier` 签名不兼容
  - 修正方式：在 `HotswapService` 默认构造路径里内联捕获 `IOException` 并转为 `UncheckedIOException`
- GREEN:
  - `./gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`：`BUILD SUCCESSFUL`
  - `./gradlew.bat :Mod:test --tests "*HotswapServiceIntegrationTest" --no-daemon`：`BUILD SUCCESSFUL`
  - `./TestMod/gradlew.bat -p . createMcpClientFiles --no-daemon`：`BUILD SUCCESSFUL`
- 文本验证：
  - `rg -n 'agentVersion|agentJarPath|moddevmcp-agent|HotswapAgent|:Agent' README.md README.zh.md docs/guides Plugin/src/main Mod/src/main TestMod settings.gradle gradle.properties -g "*.md" -g "*.java" -g "*.gradle"` 返回无匹配，说明生产代码和用户文档里已清掉本轮目标残留

## Environment Notes

- 第一次在沙盒里跑 `:Mod:test` 时，Gradle 无法访问 `Reflect:1.6.2` 仓库地址，报 `Permission denied: getsockopt`
- 该问题在提权后消失，说明这是沙盒网络限制，不是依赖坐标或 TLS/仓库配置错误
- `TestMod createMcpClientFiles` 过程中再次出现 `windows sandbox: setup refresh failed`；提权重跑后任务成功，这也是工具环境问题，不是 Gradle 任务本身失败
