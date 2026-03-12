# Playwright-Style UI Automation Checklist

Date: 2026-03-12 00:27 CST
Updated: 2026-03-12 01:21 CST

- [x] Write design and plan docs
- [x] Add session lifecycle and ref-resolution tests
- [x] Implement `ui_session_open` and `ui_session_refresh`
- [x] Implement thin ref/input/wait/screenshot tools
- [x] Add batch and trace tests
- [x] Implement `ui_batch` and `ui_trace_get`
- [x] Normalize `runtime_unavailable` / `screen_unavailable` / `session_stale` / `target_stale` / `batch_step_failed`
- [x] Add refresh-on-demand rules for ref-based actions
- [x] Fix post-review same-class stale resolution, stable batch/trace error codes, and standalone session trace coverage
- [x] Rerun focused `:Mod:test` coverage for Task 4 with `--rerun-tasks`
- [x] Record current implementation state in impl doc
- [x] Add agent-facing usage guide for the new automation flow
- [x] Update `README.md` to link the automation flow
- [x] Run real `runClient` verification for the new thin automation path
- [ ] Run one real MCP agent flow with screenshots and trace output
- [x] Record real runtime screenshot paths and trace excerpts

## Latest Verification Record

- Runtime state before flow:
  - `Get-Process java,javaw -ErrorAction SilentlyContinue | Select-Object ProcessName,Id,MainWindowTitle`
  - Result: `java 63984 Minecraft NeoForge* 1.21.1`
  - `Test-NetConnection 127.0.0.1 -Port 47653`
  - Result: `TcpTestSucceeded = True`
- Real thin-tool flow:
  - Command: `powershell -NoProfile -ExecutionPolicy Bypass -File tools\runtime\game-mcp-playwright-ui-flow.ps1`
  - Result: `Playwright-style UI flow completed.`
  - Output directory: `build/demo/playwright-ui-flow/20260312-011741`
- Real artifacts:
  - `build/demo/playwright-ui-flow/20260312-011741/step-01-title-singleplayer-button.png`
  - `build/demo/playwright-ui-flow/20260312-011741/step-02-select-world-screen.png`
  - `build/demo/playwright-ui-flow/20260312-011741/step-log.json`
- Real batch excerpt:
  - `hoverRef` success, `elapsedMs = 11`
  - `screenshot` success, `elapsedMs = 214`
  - `clickRef` success, `elapsedMs = 28`
- Scope note:
  - This confirms the real MCP thin automation path and real game-side simulated click path.
  - A separate real Codex-agent-driven flow is still pending, so that checklist item stays open.
