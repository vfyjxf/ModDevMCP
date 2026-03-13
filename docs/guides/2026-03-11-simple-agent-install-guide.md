# 2026-03-11 Simple Agent Install Guide

Date: 2026-03-11 17:30 CST
Updated: 2026-03-13 20:26 CST

## Purpose

- give MCP clients one stable host entrypoint
- keep Minecraft as a reconnecting runtime client, not an MCP host
- keep setup generic for project-local or user-level MCP configuration

See also:

- `docs/guides/2026-03-11-agent-preflight-checklist.md`
- `docs/guides/2026-03-11-agent-prompt-templates.md`

## Workflow

Host-first integration contract:

- the MCP server is `Server`
- the game connects outward to the host on `127.0.0.1:47653`
- agents should connect to `ModDevMcpStdioMain`
- the game may start later and reconnect automatically

## Setup

Start the MCP host:

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

Minimal MCP client setup:

```toml
[mcp_servers.moddevmcp]
command = 'java'
args = [
  '-cp',
  '<server-runtime-classpath>',
  'dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain',
]
```

## Plugin Consumer Notes

For NeoForge consumer projects using the Gradle plugin, prefer Maven coordinates over a hard-coded local Agent jar path:

```groovy
modDevMcp {
    agentCoordinates = "dev.vfyjxf:moddevmcp-agent:<version>"
}
```

For repository-local validation, publish first:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Agent:publishToMavenLocal :Plugin:publishToMavenLocal --no-daemon
.\TestMod\gradlew.bat -p .\TestMod createMcpClientFiles --no-daemon
```

## Verification

Agent rule:

1. start the host first
2. start the game second
3. first call `moddev.status`
4. continue only if `gameConnected=true`
5. then call `moddev.ui_get_live_screen`

If status or the first UI call fails, stop and ask the user to finish loading the game.


