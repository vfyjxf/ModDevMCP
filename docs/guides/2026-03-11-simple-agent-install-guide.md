# 2026-03-11 Simple Agent Install Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-14 14:40 CST

## Purpose

- give MCP clients one stable stdio gateway entrypoint
- keep Minecraft as a reconnecting runtime client, not an MCP host
- let agent users install from generated files instead of hand-writing Java commands

See also:

- `docs/guides/2026-03-11-game-mcp-guide.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-agent-prompt-templates.md`

## Runtime Workflow

Gateway-first integration contract:

- the MCP stdio entrypoint is `:Server:runStdioMcp`
- the gateway auto-starts the backend when needed
- the game connects outward to `127.0.0.1:47653`
- agents should connect only to the gateway
- the game may start later and reconnect automatically

## Generate Client Files

From `TestMod`:

```powershell
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat createMcpClientFiles --no-daemon
```

Generated files live under:

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

The generated launch command already includes:

- fixed `moddevmcp.host`
- fixed `moddevmcp.port`
- fixed `moddevmcp.mcpPort`
- backend bootstrap properties
- the generated `@mcp-gateway-java.args` reference

## Install By Agent

### Codex

- merge `codex.toml` into `~/.codex/config.toml`
- or run `codex mcp add` with the same generated command and args
- verify with `codex mcp list`

Official docs:

- OpenAI Codex MCP docs: https://developers.openai.com/codex/mcp/
- OpenAI Codex config reference: https://developers.openai.com/codex/config/

### Claude Code

- merge `claude-code.mcp.json` into `<project>/.mcp.json`
- or install per user with `claude mcp add --transport stdio ...`
- verify with `claude mcp list`

Official docs:

- Claude Code MCP docs: https://docs.anthropic.com/en/docs/claude-code/mcp
- Claude Code settings docs: https://docs.anthropic.com/en/docs/claude-code/settings

### Cursor

- merge `cursor-mcp.json` into `<project>/.cursor/mcp.json` or `~/.cursor/mcp.json`
- if the tools list does not refresh immediately, reopen MCP settings or restart Cursor

Official docs:

- Cursor MCP docs: https://docs.cursor.com/context/mcp

### Cline

- merge `cline_mcp_settings.json` into the Cline MCP settings file
- or use the extension UI: `MCP Servers` -> `Configure` -> `Configure MCP Servers`

Official docs:

- Cline MCP docs: https://docs.cline.bot/mcp-servers/mcp

### Windsurf

- merge `windsurf-mcp_config.json` into `~/.codeium/windsurf/mcp_config.json`
- after saving, refresh MCP servers in Cascade

Official docs:

- Windsurf MCP docs: https://docs.windsurf.com/windsurf/cascade/mcp

### VS Code

- merge `vscode-mcp.json` into `<project>/.vscode/mcp.json`
- reopen the workspace if the MCP server list stays stale

Official docs:

- VS Code MCP docs: https://code.visualstudio.com/docs/copilot/chat/mcp-servers

### Gemini CLI

- merge `gemini-settings.json` into `~/.gemini/settings.json` or `<project>/.gemini/settings.json`
- or use `gemini mcp add` with the same generated command and args

Official docs:

- Gemini CLI config docs: https://github.com/google-gemini/gemini-cli/blob/main/docs/cli/configuration.md

### Goose

- follow `goose-setup.md`
- prefer Goose command-line extension flow and paste the generated command and args directly

Official docs:

- Goose extensions docs: https://block.github.io/goose/docs/guides/using-extensions/

## Start Order

Start the gateway:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Server:runStdioMcp --no-daemon
```

Start Minecraft:

```powershell
cd TestMod
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat runClient --no-daemon
```

## Verification

Agent rule:

1. start the gateway first
2. start the game second
3. first call `moddev.status`
4. continue only if `gameConnected=true`
5. then call `moddev.ui_get_live_screen`

If status or the first UI call fails, stop and ask the user to finish loading the game.
