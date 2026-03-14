# 2026-03-11 简明 Agent 安装指南

Date: 2026-03-11 17:30 CST
Updated: 2026-03-15 00:45 CST

## 用途

- 生成可以直接安装的 MCP client 配置文件
- 让主流 agent 工具接入 ModDevMCP 时不需要手写启动命令
- 保持安装和启动流程简单

## 消费者工程配置

在你自己的 NeoForge 工程里应用插件并添加已发布的 mod 依赖：

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

插件会自动配置普通客户端接入所需的默认值。只有在你需要覆盖默认值时，才需要添加 `modDevMcp {}`：

```groovy
modDevMcp {
    runs = ["client"]
    requireEnhancedHotswap = false
}
```

## 生成 Client 文件

在你的工程中执行：

```powershell
.\gradlew.bat createMcpClientFiles --no-daemon
```

对普通消费者工程来说，这就是唯一需要手工执行的 MCP 专用 Gradle 任务。你选择的 NeoForge 运行任务会在需要时自动重新生成这些文件。

生成文件位于：

- `build/moddevmcp/mcp-clients/clients/codex.toml`
- `build/moddevmcp/mcp-clients/clients/mcp-servers.json`
- `build/moddevmcp/mcp-clients/clients/claude-code.mcp.json`
- `build/moddevmcp/mcp-clients/clients/claude-desktop.mcp.json`
- `build/moddevmcp/mcp-clients/clients/cursor-mcp.json`
- `build/moddevmcp/mcp-clients/clients/cline_mcp_settings.json`
- `build/moddevmcp/mcp-clients/clients/windsurf-mcp_config.json`
- `build/moddevmcp/mcp-clients/clients/vscode-mcp.json`
- `build/moddevmcp/mcp-clients/clients/gemini-settings.json`
- `build/moddevmcp/mcp-clients/clients/goose-setup.md`
- `build/moddevmcp/mcp-clients/clients/INSTALL.md`

## 按客户端安装

### Codex

- 把 `codex.toml` 合并到 `~/.codex/config.toml`
- 或者用生成出来的命令和参数执行 `codex mcp add`
- 用 `codex mcp list` 验证

### Claude Code

- 把 `claude-code.mcp.json` 合并到 `<project>/.mcp.json`
- 或按用户级方式执行 `claude mcp add --transport stdio ...`
- 用 `claude mcp list` 验证

### Cursor

- 把 `cursor-mcp.json` 合并到 `<project>/.cursor/mcp.json` 或 `~/.cursor/mcp.json`
- 如果工具列表没有立刻刷新，重新打开 MCP 设置或重启 Cursor

### Cline

- 把 `cline_mcp_settings.json` 合并到 Cline 的 MCP 配置文件
- 或在扩展界面使用 `MCP Servers -> Configure -> Configure MCP Servers`

### Windsurf

- 把 `windsurf-mcp_config.json` 合并到 `~/.codeium/windsurf/mcp_config.json`
- 保存后在 Cascade 里刷新 MCP servers

### VS Code

- 把 `vscode-mcp.json` 合并到 `<project>/.vscode/mcp.json`
- 如果 MCP server 列表没有刷新，重新打开工作区

### Gemini CLI

- 把 `gemini-settings.json` 合并到 `~/.gemini/settings.json` 或 `<project>/.gemini/settings.json`
- 或者用生成出来的命令和参数执行 `gemini mcp add`

### Goose

- 按 `goose-setup.md` 操作
- 在 Goose 扩展配置流程里直接粘贴生成出来的命令和参数

## 启动顺序

1. 先把生成的 MCP 配置安装到你的 MCP client
2. 再启动你平时使用的游戏运行任务，例如 `runClient`
3. 然后让 agent 连接
4. 先调用 `moddev.status`
5. 只有在 `gameConnected=true` 时才继续

MCP client 会根据安装好的配置自动启动 ModDevMCP host 入口，不需要再手工启动单独的 MCP server task。

## 相关文档

- `docs/guides/2026-03-11-game-mcp-guide.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-agent-prompt-templates.md`


