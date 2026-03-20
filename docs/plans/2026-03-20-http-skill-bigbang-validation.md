# HTTP Skill Bigbang Validation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan.

**Goal:** Validate the HTTP + skill + operation migration end-to-end in TestMod with screenshots for each step, input actions, and capture variants.

**Architecture:** Use the ModDevMCP local HTTP API (`/api/v1/status`, `/api/v1/requests`, `/api/v1/skills/*`) for readiness and execution. Drive inputs and captures via operations (`input.action`, `ui.inspect`, `ui.capture`) and collect artifacts from the capture directory.

**Tech Stack:** Gradle (`runClient`), PowerShell (`Invoke-RestMethod`), ModDevMCP HTTP API.

---

### Task 1: Verify Migration Is Clean (No Legacy MCP Tool Surface)

**Files:**
- Read: repo-wide (exclude `docs/outdated`)

**Step 1: Scan for legacy MCP/tool tokens**

Run: `rg -n "McpToolProvider|McpToolRegistry|ToolResult|ToolCallContext|tools/call|tools/list|jsonrpc|stdio mcp" D:\ProjectDir\AgentFarm\ModDevMCP\.worktrees\http-skill-bigbang-exec --glob "!docs/outdated/**"`
Expected: no matches

**Step 2: Scan for old package prefix**

Run: `rg -n "dev\\.vfyjxf\\.mcp" D:\ProjectDir\AgentFarm\ModDevMCP\.worktrees\http-skill-bigbang-exec --glob "!docs/outdated/**"`
Expected: no matches

### Task 2: Start TestMod Client And Confirm HTTP Service

**Files:**
- Read: `TestMod/build/moddevmcp/game-instances.json` (fallback)

**Step 1: Run TestMod client**

Run (from `TestMod`): `\.\gradlew.bat runClient --no-daemon`
Expected: game window opens and remains running

**Step 2: Probe default status endpoint**

Run: `Invoke-RestMethod http://127.0.0.1:47812/api/v1/status`
Expected: JSON with `serviceReady=true` and `gameReady=true`

**Step 3: If default fails, use game-instances fallback**

Run: `Get-Content D:\ProjectDir\AgentFarm\ModDevMCP\.worktrees\http-skill-bigbang-exec\TestMod\build\moddevmcp\game-instances.json`
Expected: locate `baseUrl` and repeat Step 2 with that URL

### Task 3: Export Skills And Load Entry Skill

**Files:**
- Read: `~/.moddev/skills/manifest.json` (after export)

**Step 1: Export skills**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/skills/export`
Expected: `outputDir` pointing to `~/.moddev/skills`

**Step 2: Fetch entry skill from HTTP**

Run: `Invoke-RestMethod http://127.0.0.1:47812/api/v1/skills/moddev-usage/markdown`
Expected: markdown content returned

### Task 4: Confirm Live Screen + UI Inspect

**Step 1: Live screen status**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"status.live_screen","input":{}}'`
Expected: live screen state payload

**Step 2: UI inspect**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"ui.inspect","input":{"mode":"full"}}'`
Expected: UI tree with stable selectors for targeting

**Step 3: Capture proof**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"ui.capture","input":{"source":"auto","mode":"full"}}'`
Expected: `path` + `resourceUri` returned

### Task 5: Input Action Tests (Hotkeys)

**Step 1: Open inventory (E)**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"input.action","input":{"action":"key_press","key":"key.inventory"}}'`
Expected: inventory screen opens

**Step 2: Capture proof**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"ui.capture","input":{"source":"auto","mode":"full"}}'`

**Step 3: Close inventory (ESC)**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"input.action","input":{"action":"key_press","key":"key.escape"}}'`

**Step 4: Capture proof**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"ui.capture","input":{"source":"auto","mode":"full"}}'`

### Task 6: Component Render Exports (Full / Exclude / Only)

**Step 1: Identify target selectors**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"ui.inspect","input":{"mode":"full"}}'`
Expected: choose selectors for `target` and `exclude`

**Step 2: Full export**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"ui.capture","input":{"source":"auto","mode":"full"}}'`

**Step 3: Exclude export**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"ui.capture","input":{"source":"auto","mode":"full","exclude":["<selector>"]}}'`

**Step 4: Only export**

Run: `Invoke-RestMethod -Method Post http://127.0.0.1:47812/api/v1/requests -ContentType application/json -Body '{"operationId":"ui.capture","input":{"source":"auto","mode":"crop","target":"<selector>"}}'`

### Task 7: Collect Capture Artifacts

**Step 1: List capture directory**

Run: `Get-ChildItem -Path D:\ProjectDir\AgentFarm\ModDevMCP\.worktrees\http-skill-bigbang-exec\TestMod\build\moddevmcp\captures`
Expected: capture files for each step
