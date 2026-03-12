# Capture Failure On Fallback Impl

Date: 2026-03-11

Context:
- stable server and backend attach were working
- real MCP calls were still able to reach `moddev.ui_capture`
- when runtime UI context fell back to `custom.UnknownScreen`, `UiToolProvider` still generated placeholder images
- that behavior is misleading for MCP clients because it looks like a successful screenshot

Implementation:
- changed `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- `ui_capture` now fails with `capture_unavailable` when no real offscreen or framebuffer provider matches
- removed the placeholder capture success path from `ui_capture`
- kept successful capture behavior unchanged when a real provider is present
- updated UI capture tests so unsupported fallback capture fails, while multi-target and exclude flows still pass when a test offscreen provider is registered

Verification:
- `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest.uiCaptureFailsWhenOnlyPlaceholderCaptureWouldBeAvailable" --no-daemon`
- `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --tests "*UiToolProviderTest" --no-daemon`
- `.\gradlew.bat :Server:test --tests "*StableModDevMcpServerMainTest" :Mod:test --tests "*UiToolInvocationTest" --tests "*UiToolProviderTest" --tests "*ClientRuntimeBootstrapTest" --tests "*GameBackendAgentMainTest" --tests "*EmbeddedModDevMcpStdioMainTest" --no-daemon`
- relaunched `.\gradlew.bat -Dmoddevmcp.devUiCapture=true :Mod:runClient --no-daemon`
- real MCP probe on `127.0.0.1:47653` returned:
  - `moddev.ui_snapshot` => fallback snapshot with `screenClass=custom.UnknownScreen`
  - `moddev.ui_capture(source=framebuffer)` => `capture_unavailable`
  - `moddev.ui_capture(source=offscreen)` => `capture_unavailable`

Artifacts:
- `build/demo/mcp-ui-cleanup-probe-after-failure-change.jsonl`
- `build/demo/captures/relaunch-title-screen-framebuffer.png`
- `build/demo/captures/relaunch-title-screen-offscreen.png`
