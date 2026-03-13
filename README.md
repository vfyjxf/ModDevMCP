# ModDevMCP

MCP tooling for NeoForge mod development.

## Repository Layout

- `Server`: standalone MCP host server, MCP protocol/transport glue, host scheduling, status tools
- `Mod`: Minecraft runtime integration, runtime client, built-in game tools, UI/input/capture implementations
- `TestMod`: standalone composite-build test project for real `runClient` validation

## Current Primary Architecture

The repository now uses a host-first architecture.

- agents connect only to `Server`
- the MCP stdio entrypoint is `dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain`
- the Minecraft client does not host MCP directly
- `Mod` starts a runtime client and reconnects to the host in the background
- runtime tools appear dynamically after the game connects
- if the game is offline, host-owned tools still respond with explicit status such as `hostReady=true`

Default host endpoint for the game runtime client:

- host: `127.0.0.1`
- port: `47653`

Optional JVM overrides:

- `moddevmcp.host`
- `moddevmcp.port`

## Primary Real-Game Workflow

1. Start the MCP host from `Server`.
2. Start Minecraft from `TestMod`.
3. Let the client reconnect to the host automatically.
4. Call `moddev.status` first.
5. Only use game tools after `gameConnected=true`.

Manual host startup for repository debugging:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Server:runStdioMcp --no-daemon
```

Real client startup:

```powershell
cd TestMod
$env:GRADLE_USER_HOME='..\.gradle-user'
.\gradlew.bat runClient --no-daemon
```

What `runClient` does:

- starts the standalone NeoForge client
- loads `mod_dev_mcp`
- initializes runtime providers inside the game process
- starts a background reconnect loop to `127.0.0.1:47653`

## Plugin Consumer Notes

`modDevMcp` resolves the hotswap agent from Maven by default instead of assuming a repository-local jar path.

Typical consumer configuration:

```groovy
modDevMcp {
    agentCoordinates = "dev.vfyjxf:moddevmcp-agent:<version>"
}
```

Repository-local validation flow:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Agent:publishToMavenLocal :Plugin:publishToMavenLocal --no-daemon
.\TestMod\gradlew.bat -p .\TestMod createMcpClientFiles --no-daemon
```

## MCP Client Connection

For MCP clients such as Codex, Claude Code, Gemini CLI, or Goose, point the client at the `Server` stdio entrypoint.

Generic command shape:

```toml
[mcp_servers.moddevmcp]
command = 'java'
args = [
  '-cp',
  '<server-runtime-classpath>',
  'dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain',
]
```

Repository-local Gradle task for manual debugging:

- `:Server:runStdioMcp`

Optional listener-only debug task:

- `:Server:runServer`

`runServer` only starts the host runtime listener. It is not the MCP stdio entrypoint used by agents.

## Agent Rule

Use this operator rule:

1. start the MCP host
2. start Minecraft
3. wait until the target UI is ready
4. call `moddev.status`
5. continue only if `gameConnected=true`
6. then call `moddev.ui_get_live_screen`

If status or the first UI probe fails, stop and tell the user in Chinese:

- `请先启动并加载游戏，再继续使用 ModDevMCP。`

Prompt templates:

- `docs/guides/2026-03-11-agent-prompt-templates.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.md`

## Implemented Runtime Capabilities

- host-owned status reporting via `moddev.status`
- runtime tool refresh on game connect/disconnect
- UI snapshot/query/capture/action/wait/tooltip
- interaction state and target details
- live screen probe via `moddev.ui_get_live_screen`
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

## Verification Notes

Recent real validation covered:

- sequential JSON-line MCP requests against `ModDevMcpStdioMain`
- `moddev.status` over a real Codex MCP session
- `TestMod` `runClient` startup with runtime reconnect logs
- host-aware server regression tests in `Server`

If Gradle dependency downloads fail, treat TLS/repository/network failures separately from code failures.


