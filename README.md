# ModDevMCP

MCP tooling for Minecraft NeoForge mod development.

This repository provides a host MCP server plus in-game runtime integration so agents can inspect UI state, capture screenshots, query live screens, and drive common debug workflows inside Minecraft.

## Overview

- add a published mod dependency to your NeoForge project
- apply the Gradle plugin `dev.vfyjxf.moddevmcp`
- generate MCP client config files from your own project
- install the generated config into your MCP client
- start your normal game run and use live MCP tools after the game is ready

## Architecture

- your MCP client starts the generated ModDevMCP host entry
- the host is the stable MCP endpoint and always provides `moddev.status`
- the game runtime connects back to the host after Minecraft starts
- client and server runtime tools appear dynamically after their runtime is connected

## Main Tools

- `status / game`: `moddev.status`, `moddev.game_close`
- `ui high-level`: `moddev.ui_get_live_screen`, `moddev.ui_run_intent`, `moddev.ui_inspect`, `moddev.ui_act`, `moddev.ui_wait`, `moddev.ui_screenshot`, `moddev.ui_trace_recent`
- `ui low-level`: `moddev.ui_session_open`, `moddev.ui_session_refresh`, `moddev.ui_click_ref`, `moddev.ui_hover_ref`, `moddev.ui_press_key`, `moddev.ui_type_text`, `moddev.ui_wait_for`, `moddev.ui_batch`, `moddev.ui_trace_get`, `moddev.ui_switch`, `moddev.ui_close`
- `state / capture / inventory / dev`: `moddev.ui_snapshot`, `moddev.ui_query`, `moddev.ui_capture`, `moddev.ui_action`, `moddev.ui_inspect_at`, `moddev.ui_get_tooltip`, `moddev.ui_get_interaction_state`, `moddev.ui_get_target_details`, `moddev.inventory_snapshot`, `moddev.inventory_action`, `moddev.event_poll`, `moddev.event_subscribe`, `moddev.compile`, `moddev.hotswap`

## Quick Start

1. Add ModDevMCP to your NeoForge project.
2. Run `createMcpClientFiles` in your project.
3. Install the generated config into your MCP client.
4. Start your normal game run, for example `runClient`.
5. Call `moddev.status`.
6. Continue only if `gameConnected=true`.

## Add to Your Project

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

The plugin owns the default MCP wiring. For a normal client setup you do not need a `modDevMcp {}` block.

Only add `modDevMcp {}` when you need to override defaults:

```groovy
modDevMcp {
    runs = ["client"]
    requireEnhancedHotswap = false
}
```

## Generate MCP Client Files

From your project:

```powershell
.\gradlew.bat createMcpClientFiles --no-daemon
```

For a normal consumer project, that is the only MCP-specific Gradle task you need to run manually. Your selected NeoForge run tasks keep these generated files in sync automatically.

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

## Install the Generated Config

- merge the generated file for your MCP client into its config
- or use the generated command and arguments with your MCP client's install command
- use the generated files under `build/moddevmcp/mcp-clients/clients/`

## Start Your Game

Start your normal NeoForge development run:

```powershell
.\gradlew.bat runClient --no-daemon
```

Use the generated MCP client config to start the ModDevMCP host entry. The MCP client launches the host entry for you, so you do not need a separate server task.

## First Readiness Check

Use this order:

1. connect the agent to ModDevMCP
2. call `moddev.status`
3. continue only if `gameConnected=true`
4. call `moddev.ui_get_live_screen`
5. continue only if that call succeeds

If MCP connection fails, or the first status/UI call fails, treat the game as not ready.

## Guides

- `docs/guides/2026-03-11-simple-agent-install-guide.md`
- `docs/guides/2026-03-11-game-mcp-guide.md`
- `docs/guides/2026-03-11-testmod-runclient-guide.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-agent-prompt-templates.md`
- `docs/guides/2026-03-11-codex-screenshot-demo-guide.md`
- `docs/guides/2026-03-11-live-screen-tool-guide.md`
- `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
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
- `docs/guides/2026-03-15-moddevmcp-usage-skill-install.zh.md`


