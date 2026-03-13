# Codex Screenshot Demo Guide

Date: 2026-03-11 17:30 CST

Purpose:

- connect a Codex-style MCP client to the game MCP entrypoint
- capture and preserve real screenshots through MCP

Setup:

```powershell
cd TestMod
.\gradlew.bat runClient --no-daemon
.\gradlew.bat :Mod:createGameMcpBridgeLaunchScript --no-daemon
```

Recommended Codex MCP config:

```toml
[mcp_servers.moddevmcp]
command = 'D:\\ProjectDir\\AgentFarm\\ModDevMCP\\Mod\\build\\moddevmcp\\game-mcp\\run-game-mcp-bridge.bat'
```

Recommended first call:

- `moddev.ui_get_live_screen`

Then use:

- `moddev.ui_snapshot`
- `moddev.ui_capture`

Example capture arguments:

```json
{
  "screenClass": "net.minecraft.client.gui.screens.TitleScreen",
  "modId": "minecraft",
  "mouseX": 0,
  "mouseY": 0,
  "source": "framebuffer"
}
```
