# ModDevMCP

MCP for NeoForge mod development.

Current layout:

- `Server`: reusable MCP protocol, registry, transport, and SDK glue
- `Mod`: Minecraft runtime, drivers, built-in tools, capture/input logic, and game-side entrypoints
- `TestMod`: standalone composite-build test project for real `runClient` validation

## Current Primary Architecture

The primary runtime path is now `game MCP`.

That means:

- `runClient` starts Minecraft
- the Minecraft client process starts the MCP endpoint inside the same JVM
- the embedded MCP bootstrap/runtime implementation now lives in `Server`
- `Mod` prepares Minecraft runtime providers and calls the `Server` bootstrap
- external agents connect only after the game is already running
- there is no stable-server/backend chain in the primary workflow

Default game MCP endpoint:

- host: `127.0.0.1`
- port: `47653`

Optional JVM overrides:

- `moddevmcp.host`
- `moddevmcp.port`

## Primary Real-Game Workflow

Run the client from `TestMod`:

```powershell
cd TestMod
.\gradlew.bat runClient --no-daemon
```

This is the primary real-game validation path.

What it does:

- starts the standalone NeoForge client
- loads `mod_dev_mcp`
- starts the game MCP socket inside the client process

## MCP Client Connection

For command-based MCP clients such as Codex, Claude Code, Gemini CLI, or Goose, use the connect-only bridge:

```powershell
.\gradlew.bat :Mod:createGameMcpBridgeLaunchScript --no-daemon
```

Generated files:

- `Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat`
- `Mod\build\moddevmcp\game-mcp\game-mcp-bridge-java.args`

Bridge bootstrap class:

- `dev.vfyjxf.mcp.bootstrap.GameMcpBridgeMain`

Example MCP config:

```toml
[mcp_servers.moddevmcp]
command = '<repo>\\Mod\\build\\moddevmcp\\game-mcp\\run-game-mcp-bridge.bat'
```

The bridge only connects to an already running game MCP socket.

It does not:

- start Minecraft
- start any separate MCP daemon for you
- infer that the game is ready without a real tool check

## Agent Rule

Use this operator rule:

1. start Minecraft first
2. wait for the title screen or target world/UI to be ready
3. start the MCP client
4. first verify the MCP connection
5. then use game tools

Recommended first tool call after connection:

- `moddev.ui_get_live_screen`

If MCP connection fails, the agent should stop and tell the user in Chinese:

- `请先启动并加载游戏，再继续使用 ModDevMCP。`

Prompt templates:

- `docs/guides/2026-03-11-agent-prompt-templates.md`
- `docs/guides/2026-03-11-game-mcp-guide.md`

## Implemented Runtime Capabilities

- UI snapshot/query/capture/action/wait/tooltip
- interaction state and target details
- live screen probe via `moddev.ui_get_live_screen`
- explicit high-level UI entry via `moddev.ui_run_intent` for `inventory`, `chat`, and `pause_menu`
- high-level Playwright-style debug flow via:
  - `moddev.ui_inspect`
  - `moddev.ui_act`
  - `moddev.ui_wait`
  - `moddev.ui_screenshot`
  - `moddev.ui_trace_recent`
- low-level session/ref automation via:
  - `moddev.ui_session_open`
  - `moddev.ui_session_refresh`
  - `moddev.ui_click_ref`
  - `moddev.ui_hover_ref`
  - `moddev.ui_switch`
  - `moddev.ui_press_key`
  - `moddev.ui_type_text`
  - `moddev.ui_wait_for`
  - `moddev.ui_batch`
  - `moddev.ui_trace_get`
- inventory snapshot/action
- in-process client input simulation
- framebuffer and offscreen capture providers

If no real capture provider matches, `moddev.ui_capture` returns a real failure instead of a fake screenshot.

Automation usage notes:

- `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- `docs/plans/2026-03-12-playwright-style-ui-automation/impl.md`
- `docs/plans/2026-03-12-ui-driver-playwright-debug-api/impl.md`

## Repository Development Entry Points

Protocol-only `Server` stdio example:

```toml
[mcp_servers.moddevmcp_protocol]
command = 'java'
args = [
  '-cp',
  '<repo>\\Server\\build\\libs\\<server-jar>.jar',
  'dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain',
]
```

That validates the protocol host itself, but it does not expose Minecraft runtime tools.

Legacy standalone embedded stdio host for repository debugging:

```powershell
.\gradlew.bat :Mod:createEmbeddedMcpLaunchScript --no-daemon
```

Outputs:

- `Mod\build\moddevmcp\embedded-mcp\run-embedded-mcp-stdio.bat`
- `Mod\build\moddevmcp\embedded-mcp\embedded-mcp-java.args`

This path is for low-level debugging, not the primary real-game workflow.

## Real Artifacts

Recent real verification artifacts live under:

- `build\demo\live-screen`
- `build\demo\playwright-ui-flow`
- `TestMod\run\build\moddevmcp\captures`

If Gradle dependency downloads fail, treat repository/TLS/network failures separately from code failures.
