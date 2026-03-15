# ModDevMCP

面向 Minecraft NeoForge Mod 开发的 MCP 调试工具集。

这个仓库提供一个主机侧 MCP 服务，以及游戏内运行时集成，让 agent 可以在 Minecraft 中检查 UI 状态、截图、读取 live screen，并执行常见的调试交互流程。

## 概览

- 在你的 NeoForge 工程里添加已发布的 mod 依赖
- 应用 Gradle 插件 `dev.vfyjxf.moddevmcp`
- 在你自己的工程里生成 MCP client 配置文件
- 把生成的配置安装到你的 MCP client
- 启动你平时使用的游戏运行任务，并在游戏就绪后使用 MCP 工具

## 架构

- 你的 MCP client 通过生成出来的 ModDevMCP host 入口建立连接
- host 是稳定的 MCP 入口，并始终提供 `moddev.status`
- Minecraft 启动后，游戏运行时会主动回连到 host
- client 和 server 两侧的运行时工具会在各自连接成功后动态出现

## 主要工具

- `status / game`：`moddev.status`、`moddev.game_close`
- `ui high-level`：`moddev.ui_get_live_screen`、`moddev.ui_run_intent`、`moddev.ui_inspect`、`moddev.ui_act`、`moddev.ui_wait`、`moddev.ui_screenshot`、`moddev.ui_trace_recent`
- `ui low-level`：`moddev.ui_session_open`、`moddev.ui_session_refresh`、`moddev.ui_click_ref`、`moddev.ui_hover_ref`、`moddev.ui_press_key`、`moddev.ui_type_text`、`moddev.ui_wait_for`、`moddev.ui_batch`、`moddev.ui_trace_get`、`moddev.ui_switch`、`moddev.ui_close`
- `state / capture / inventory / dev`：`moddev.ui_snapshot`、`moddev.ui_query`、`moddev.ui_capture`、`moddev.ui_action`、`moddev.ui_inspect_at`、`moddev.ui_get_tooltip`、`moddev.ui_get_interaction_state`、`moddev.ui_get_target_details`、`moddev.inventory_snapshot`、`moddev.inventory_action`、`moddev.event_poll`、`moddev.event_subscribe`、`moddev.compile`、`moddev.hotswap`

## 快速开始

1. 在你的 NeoForge 工程里接入 ModDevMCP。
2. 在你的工程里执行 `createMcpClientFiles`。
3. 把生成的配置安装到你的 MCP client。
4. 启动你平时使用的游戏运行任务，例如 `runClient`。
5. 调用 `moddev.status`。
6. 只有在 `gameConnected=true` 时才继续。

## 已发布坐标

- Mod 坐标：`dev.vfyjxf:moddevmcp:<version>`
- Server 坐标：`dev.vfyjxf:moddevmcp-server:<version>`
- Gradle 插件 id：`dev.vfyjxf.moddevmcp`

对普通消费者工程来说，只需要声明 mod 坐标并应用插件。插件会自动解析用于 MCP host 生成的 server 依赖。

## 接入你的工程

```groovy
plugins {
    id 'net.neoforged.moddev' version '<moddevgradle-version>'
    id 'dev.vfyjxf.moddevmcp' version '<moddevmcp-version>'
}

dependencies {
    implementation("dev.vfyjxf:moddevmcp:<version>") {
        transitive = false
    }
}

```

通常不需要手工声明 `dev.vfyjxf:moddevmcp-server:<version>`。

插件会接管默认的 MCP 配置。普通的客户端接入不需要再写 `modDevMcp {}`。

只有在你需要覆盖默认值时，才需要显式添加 `modDevMcp {}`：

```groovy
modDevMcp {
    runs = ["client"]
    requireEnhancedHotswap = false
}
```

## 生成 MCP Client 配置

在你的工程中执行：

```powershell
.\gradlew.bat createMcpClientFiles --no-daemon
```

对普通消费者工程来说，这就是唯一需要手工执行的 MCP 专用 Gradle 任务。你选择的 NeoForge 运行任务会自动保持这些生成文件同步。

生成的文件位于：

- `build/moddevmcp/mcp-clients/clients/codex.toml`
- `build/moddevmcp/mcp-clients/clients/claude-code.mcp.json`
- `build/moddevmcp/mcp-clients/clients/cursor-mcp.json`
- `build/moddevmcp/mcp-clients/clients/vscode-mcp.json`
- `build/moddevmcp/mcp-clients/clients/gemini-settings.json`
- `build/moddevmcp/mcp-clients/clients/INSTALL.md`

## 安装生成的配置

- 把对应 MCP client 的生成文件合并到它的配置里
- 或直接用生成出来的命令和参数执行对应客户端的安装命令
- 使用 `build/moddevmcp/mcp-clients/clients/` 下的生成文件
- 只有已经核对过官方格式的客户端才会生成专用配置文件

## 启动游戏

启动你平时使用的 NeoForge 开发运行任务：

```powershell
.\gradlew.bat runClient --no-daemon
```

使用生成好的 MCP client 配置来启动 ModDevMCP host 入口。MCP client 会替你拉起这个 host 入口，因此不需要单独启动 server task。

## 首次就绪检查

推荐顺序：

1. 让 agent 连接到 ModDevMCP
2. 调用 `moddev.status`
3. 只有在 `gameConnected=true` 时才继续
4. 调用 `moddev.ui_get_live_screen`
5. 只有在该调用成功时才继续

如果 MCP 连接失败，或首次状态 / UI 调用失败，就把游戏视为尚未就绪。

## 文档

- `docs/guides/2026-03-11-simple-agent-install-guide.md`
- `docs/guides/2026-03-11-game-mcp-guide.md`
- `docs/guides/2026-03-11-testmod-runclient-guide.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-agent-prompt-templates.md`
- `docs/guides/2026-03-11-codex-screenshot-demo-guide.md`
- `docs/guides/2026-03-11-live-screen-tool-guide.md`
- `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- `docs/guides/2026-03-15-third-party-mod-integration-guide.md`
- `docs/guides/2026-03-15-moddevmcp-usage-skill-install.md`
- `README.zh.md`
- `docs/guides/2026-03-11-simple-agent-install-guide.zh.md`
- `docs/guides/2026-03-11-game-mcp-guide.zh.md`
- `docs/guides/2026-03-11-testmod-runclient-guide.zh.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.zh.md`
- `docs/guides/2026-03-11-agent-prompt-templates.zh.md`
- `docs/guides/2026-03-11-codex-screenshot-demo-guide.zh.md`
- `docs/guides/2026-03-11-live-screen-tool-guide.zh.md`
- `docs/guides/2026-03-12-playwright-style-ui-automation-guide.zh.md`
- `docs/guides/2026-03-15-third-party-mod-integration-guide.zh.md`
- `docs/guides/2026-03-15-moddevmcp-usage-skill-install.zh.md`


