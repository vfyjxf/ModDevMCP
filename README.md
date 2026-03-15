# ModDevMCP

MCP tooling for Minecraft NeoForge mod development.

This repository provides a host MCP server plus in-game runtime integration so agents can inspect UI state, capture screenshots, query live screens, and drive common debug workflows inside Minecraft.

The project uses a host-first architecture: the stable MCP host starts first, then Minecraft connects its runtime back to that host.

## Overview

- add a published mod dependency to your NeoForge project
- apply the Gradle plugin `dev.vfyjxf.moddevmcp`
- generate MCP client config files from your own project
- install the generated config into your MCP client
- start your normal game run and use live MCP tools after the game is ready

## Architecture

- the primary host entry point is `ModDevMcpStdioMain`
- you can run the host directly with `:Server:runStdioMcp`
- generated MCP client config files start the same host entry for normal agent usage
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

If the host is already running but Minecraft has not connected yet, `moddev.status` still reports `hostReady=true`.

## Published Artifacts

- Mod artifact: `dev.vfyjxf:moddevmcp:<version>`
- Server artifact: `dev.vfyjxf:moddevmcp-server:<version>`
- Gradle plugin id: `dev.vfyjxf.moddevmcp`

For a normal consumer project, declare the mod artifact and apply the plugin. The plugin resolves the server artifact for MCP host generation automatically.

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

You normally do not need to declare `dev.vfyjxf:moddevmcp-server:<version>` yourself.

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
- `build/moddevmcp/mcp-clients/clients/claude-code.mcp.json`
- `build/moddevmcp/mcp-clients/clients/cursor-mcp.json`
- `build/moddevmcp/mcp-clients/clients/vscode-mcp.json`
- `build/moddevmcp/mcp-clients/clients/gemini-settings.json`
- `build/moddevmcp/mcp-clients/clients/INSTALL.md`

## Install the Generated Config

- merge the generated file for your MCP client into its config
- or use the generated command and arguments with your MCP client's install command
- use the generated files under `build/moddevmcp/mcp-clients/clients/`
- only officially verified client formats are generated

## Start Your Game

Start your normal NeoForge development run:

```powershell
.\gradlew.bat runClient --no-daemon
```

Use the generated MCP client config to start the ModDevMCP host entry. For direct debugging you can also start the same host manually with `:Server:runStdioMcp`, which runs `ModDevMcpStdioMain`.

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
- `docs/guides/2026-03-15-command-tools-guide.md`
- `docs/guides/2026-03-15-local-world-tools-guide.md`
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


Use this operator rule:

1. start the MCP gateway
2. start Minecraft
3. wait until the target UI is ready
4. call `moddev.status`
5. continue only if `gameConnected=true`
6. then call `moddev.ui_get_live_screen`

If status or the first UI probe fails, stop and tell the user in Chinese:

- `请先启动并加载游戏，再继续使用 ModDevMCP。`

## Implemented Runtime Capabilities

- host-owned status reporting via `moddev.status`
- runtime tool refresh on game connect/disconnect
- UI snapshot/query/capture/action/wait/tooltip
- interaction state and target details
- live screen probe via `moddev.ui_get_live_screen`
- command discovery / suggestion / execution via `moddev.command_list`, `moddev.command_suggest`, `moddev.command_execute`
- local world list / create / join via `moddev.world_list`, `moddev.world_create`, `moddev.world_join` with configurable world type, difficulty, bonus chest, and structure generation
- client option control via `moddev.pause_on_lost_focus`
- high-level Playwright-style debug flow via `moddev.ui_inspect`, `moddev.ui_act`, `moddev.ui_wait`, `moddev.ui_screenshot`, `moddev.ui_trace_recent`
- low-level session/ref automation via `moddev.ui_session_open`, `moddev.ui_session_refresh`, `moddev.ui_click_ref`, `moddev.ui_hover_ref`, `moddev.ui_switch`, `moddev.ui_press_key`, `moddev.ui_type_text`, `moddev.ui_wait_for`, `moddev.ui_batch`, `moddev.ui_trace_get`
- inventory snapshot/action
- in-process client input simulation
- framebuffer and offscreen capture providers

If no real capture provider matches, `moddev.ui_capture` returns a real failure instead of a fake screenshot.

## Verification Notes

Recent real validation covered:

- gateway auto-bootstrap of backend from generated client config
- `moddev.status` over a real Codex-equivalent MCP session
- `TestMod` `runClient` startup with runtime reconnect logs
- host-aware server regression tests in `Server`

If Gradle dependency downloads fail, treat TLS/repository/network failures separately from code failures.
