# ModDevMCP

MCP tooling for NeoForge mod development.

## Repository Layout

- `Server`: standalone MCP gateway, backend bootstrap, transport glue, host-owned tools
- `Mod`: Minecraft runtime client, built-in game tools, UI/input/capture implementations
- `Plugin`: Gradle plugin that injects runtime settings and generates MCP client install files
- `TestMod`: composite-build test project for real `runClient` validation

## Current Primary Architecture

The repository now uses a host-first architecture with a standalone gateway and backend.

- agents connect only to the `Server` stdio gateway
- the gateway can auto-start the backend if it is not already running
- the backend is the stable state center for MCP clients and Minecraft runtime clients
- the Minecraft client never hosts MCP directly
- `Mod` starts a runtime client and reconnects to the backend in the background
- runtime tools appear dynamically after the game connects
- if the game is offline, host-owned tools still respond with explicit status such as `hostReady=true`

Default runtime endpoint:

- host: `127.0.0.1`
- backend port: `47653`
- MCP proxy port: `47654`

Optional JVM overrides:

- `moddevmcp.host`
- `moddevmcp.port`
- `moddevmcp.mcpPort`
- `moddevmcp.backend.javaCommand`
- `moddevmcp.backend.argsFile`
- `moddevmcp.backend.launcher`

## Primary Real-Game Workflow

1. Start the MCP gateway from `Server`.
2. Start Minecraft from `TestMod`.
3. Let the game reconnect to the backend automatically.
4. Call `moddev.status` first.
5. Only use game tools after `gameConnected=true`.

Manual gateway startup for repository debugging:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Server:runStdioMcp --no-daemon
```

That task boots the stdio gateway entry point `dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain`.

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
- reconnects to `127.0.0.1:47653` in the background
- depends on `createMcpClientFiles`, so generated MCP configs stay in sync with the current build

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

Generated client files are written under:

- `TestMod/build/moddevmcp/mcp-clients/clients/codex.toml`
- `TestMod/build/moddevmcp/mcp-clients/clients/mcp-servers.json`
- `TestMod/build/moddevmcp/mcp-clients/clients/INSTALL.md`

## MCP Client Connection

For MCP clients such as Codex, Claude Code, Cursor, Cline, Windsurf, VS Code, Gemini CLI, or Goose, reuse the generated files from `createMcpClientFiles` instead of hand-writing a Java command.

Repository-local Gradle task for manual debugging:

- `:Server:runStdioMcp`

Optional listener-only debug task:

- `:Server:runServer`

`runServer` starts the backend runtime listener only. It is not the stdio gateway used by agents.

Install and usage guides:

- `docs/guides/2026-03-11-simple-agent-install-guide.md`
- `docs/guides/2026-03-11-game-mcp-guide.md`
- `docs/guides/2026-03-11-testmod-runclient-guide.md`
- `docs/guides/2026-03-11-agent-preflight-checklist.md`

## Agent Rule

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
