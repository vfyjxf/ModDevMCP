# 2026-03-11 Simple Agent Install Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-15 00:45 CST

## Purpose

- generate ready-to-install MCP client config files
- install ModDevMCP into mainstream agent tools without hand-writing commands
- keep the install and startup flow simple

## Consumer Project Setup

Apply the plugin and add the published mod dependency in your own NeoForge project:

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

The plugin configures the normal client defaults for you. Add `modDevMcp {}` only when you need to override something:

```groovy
modDevMcp {
    runs = ["client"]
    requireEnhancedHotswap = false
}
```

## Generate Client Files

From your project:

```powershell
.\gradlew.bat createMcpClientFiles --no-daemon
```

For a normal consumer project, that is the only MCP-specific Gradle task you need to run manually. Your selected NeoForge run tasks regenerate these files automatically when needed.

Generated files are written under:

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

## Install by Client

### Codex

- merge `codex.toml` into `~/.codex/config.toml`
- or use `codex mcp add` with the generated command and arguments
- verify with `codex mcp list`

### Claude Code

- merge `claude-code.mcp.json` into `<project>/.mcp.json`
- or install it per user with `claude mcp add --transport stdio ...`
- verify with `claude mcp list`

### Cursor

- merge `cursor-mcp.json` into `<project>/.cursor/mcp.json` or `~/.cursor/mcp.json`
- if the tool list does not refresh immediately, reopen MCP settings or restart Cursor

### Cline

- merge `cline_mcp_settings.json` into the Cline MCP settings file
- or use `MCP Servers -> Configure -> Configure MCP Servers`

### Windsurf

- merge `windsurf-mcp_config.json` into `~/.codeium/windsurf/mcp_config.json`
- refresh MCP servers in Cascade after saving

### VS Code

- merge `vscode-mcp.json` into `<project>/.vscode/mcp.json`
- reopen the workspace if MCP servers stay stale

### Gemini CLI

- merge `gemini-settings.json` into `~/.gemini/settings.json` or `<project>/.gemini/settings.json`
- or use `gemini mcp add` with the generated command and arguments

### Goose

- follow `goose-setup.md`
- paste the generated command and arguments into the Goose extension setup flow

## Start Order

1. install the generated MCP config into your MCP client
2. start your normal game run, such as `runClient`
3. connect the agent
4. call `moddev.status`
5. continue only if `gameConnected=true`

The MCP client starts the generated ModDevMCP host entry from its installed config. You do not need to launch a separate MCP server task by hand.

## Related Guides

- `docs/guides/2026-03-11-game-mcp-guide.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-agent-prompt-templates.md`


