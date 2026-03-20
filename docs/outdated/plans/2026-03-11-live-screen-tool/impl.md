# Live Screen Tool Implementation

Date: 2026-03-11 16:40 CST

## Scope

This step adds a lightweight runtime tool:

- `moddev.ui_get_live_screen`

The tool returns current client screen identity and dimensions without requiring a full UI snapshot first.

## Code Changes

- Added `ClientScreenMetrics` and `ClientScreenProbe`:
  - `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/ClientScreenMetrics.java`
  - `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/ClientScreenProbe.java`
- Added live Minecraft-backed probe:
  - `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/LiveClientScreenProbe.java`
- Wired the probe into `ModDevMCP` and `UiToolProvider`
- Added `moddev.ui_get_live_screen` definition, registration, and payload mapping
- Added stable-server metadata entry for the new tool
- Added/updated tests for:
  - provider registration
  - invocation payload
  - Mod bootstrap registration
  - backend agent response sequencing

## Real Verification

### Unit / focused Gradle verification

Verified with:

```powershell
.\gradlew.bat :Mod:test --tests "*UiToolProviderTest" --tests "*UiToolInvocationTest" --tests "*BuiltinProviderRegistrationTest" --tests "*ClientRuntimeBootstrapTest" --tests "*GameBackendAgentMainTest" --tests "*GameBackendAgentHeartbeatTest" --no-daemon
.\gradlew.bat :Server:test --tests "*StableModDevMcpServerMainTest" --no-daemon
```

Real result:

- both commands succeeded

### Real `runClient` verification

Cold-started `TestMod:runClient`, then queried the stable server.

Observed runtime status:

- `moddev://runtime/status` returned `connected`

Observed live-screen tool result:

```json
{
  "active": true,
  "screenClass": "net.minecraft.client.gui.screens.TitleScreen",
  "modId": "minecraft",
  "driverId": "vanilla-screen",
  "guiWidth": 427,
  "guiHeight": 240,
  "framebufferWidth": 854,
  "framebufferHeight": 480
}
```

Also saved a real framebuffer capture after the live-screen check:

- response file:
  - `build/demo/live-screen/ui-capture-response.json`
- PNG file:
  - `TestMod/run/build/moddevmcp/captures/capture-1.png`

## Important Finding

The first real failure was not caused by Mod registration.

What actually happened:

- backend game process already had `moddev.ui_get_live_screen`
- a long-lived old stable-server process on `127.0.0.1:47653` was still running older code
- MCP calls therefore hit stale server metadata/dispatch state and returned `Method not found`

Real fix path:

1. stop the stale stable-server process
2. stop the attached Minecraft client
3. cold-start `runClient` so it auto-starts the fresh stable server again

After that, the tool worked end to end.
