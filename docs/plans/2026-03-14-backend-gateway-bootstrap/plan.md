# Backend Gateway Bootstrap Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** replace the current stdio-owned host lifecycle with a stable backend plus an auto-bootstrapping stdio gateway, and make plugin-generated install files the default supported path.

**Architecture:** `Server` will expose separate backend and gateway mains. The backend owns runtime connectivity and state. The gateway auto-starts the backend when required, waits for readiness, then serves MCP stdio while proxying to backend. `Mod` keeps reconnect-only behavior and the plugin generates launchers plus agent install files that point to the gateway.

**Tech Stack:** Java 21, Gradle, NeoForge ModDevGradle, Gson, MCP SDK core, JUnit 5

---

### Task 1: Add backend/gateway bootstrap tests

**Files:**
- Modify: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpStdioMainTest.java`
- Create: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpGatewayBootstrapTest.java`
- Create: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/BackendAvailabilityProbeTest.java`

**Step 1: Write failing tests**

- add a test proving gateway fails clearly when backend is unavailable
- add a test proving gateway can attach to an already running backend
- add a test proving backend lifecycle is independent from stdio EOF

**Step 2: Run tests to verify they fail**

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Server:test --tests "*GatewayBootstrapTest" --tests "*BackendAvailabilityProbeTest" --tests "*ModDevMcpStdioMainTest" --no-daemon
```

Expected: FAIL because gateway/backend split is not implemented yet.

**Step 3: Implement minimal bootstrap support**

- add backend readiness probe
- stop using stdio lifecycle as backend lifecycle
- add gateway startup error mapping

**Step 4: Re-run the same tests**

Expected: PASS

### Task 2: Split backend and gateway mains in `Server`

**Files:**
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpBackendMain.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpGatewayMain.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/BackendProcessLauncher.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/BackendAvailabilityProbe.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpServerFactory.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpStdioMain.java`
- Modify: `Server/build.gradle`

**Step 1: Implement backend main**

- backend main starts runtime listener and backend-facing server components only
- backend main stays alive independently of stdio

**Step 2: Implement gateway main**

- gateway probes backend
- if needed, gateway launches backend
- gateway waits for readiness
- gateway then serves stdio MCP

**Step 3: Keep old entrypoint as compatibility wrapper**

- `ModDevMcpStdioMain` should become a thin compatibility alias to the new gateway path or be deprecated internally

**Step 4: Verify with targeted server tests**

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Server:test --no-daemon
```

Expected: PASS

### Task 3: Update plugin-generated launch files

**Files:**
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpExtension.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpPlugin.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/CreateMcpClientFilesTask.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/McpLaunchFiles.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/neoforge/NeoForgeRunInjector.java`
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/CreateMcpClientFilesTaskTest.java`
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/NeoForgeRunInjectorTest.java`
- Create: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/McpGatewayLaunchFilesTest.java`

**Step 1: Write failing plugin tests**

- generated MCP client configs should point to the gateway launcher
- generated files should include backend launcher files
- run injection should still write fixed backend host/port into `runClient`

**Step 2: Run plugin tests and watch them fail**

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Plugin:test --tests "*CreateMcpClientFilesTaskTest" --tests "*McpGatewayLaunchFilesTest" --tests "*NeoForgeRunInjectorTest" --no-daemon
```

Expected: FAIL until generation logic is updated.

**Step 3: Update generation logic**

- emit backend args and launchers
- emit gateway args and launchers
- emit client snippets that invoke gateway launcher

**Step 4: Re-run plugin tests**

Expected: PASS

### Task 4: Keep `Mod` reconnect-only and verify real consumer wiring

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/host/HostReconnectLoop.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/host/HostRuntimeClientConfig.java`
- Modify: `TestMod/build.gradle`
- Modify: `TestMod/gradle.properties` if needed

**Step 1: Add failing reconnect or config tests if needed**

- confirm client still uses fixed backend endpoint
- confirm reconnect loop remains background-only

**Step 2: Make minimal runtime adjustments**

- keep reconnect semantics
- improve logging for backend attach and detach

**Step 3: Run focused mod tests**

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Mod:test --tests "*HostArchitectureDocsTest" --no-daemon
```

Expected: PASS

### Task 5: Update install guides and validate real flow

**Files:**
- Modify: `README.md`
- Modify: `docs/guides/2026-03-11-game-mcp-guide.md`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.md`
- Modify: `docs/guides/2026-03-11-testmod-runclient-guide.md`
- Add or modify any generated config guide under `docs/guides`

**Step 1: Update docs**

- document backend + gateway architecture
- document which generated files are for humans and which are for agents
- add install/use instructions for Codex, Claude Code, Cursor, Cline, Gemini CLI, Goose, and VS Code

**Step 2: Run real validation**

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'
.\gradlew.bat :Plugin:test --no-daemon
.\TestMod\gradlew.bat -p .\TestMod createMcpClientFiles --no-daemon
.\TestMod\gradlew.bat -p .\TestMod runClient --no-daemon
```

Then validate:

- generated MCP config starts gateway
- gateway starts backend automatically
- game reconnects to backend
- `moddev.status` reports `gameConnected=true`

**Step 3: Record real outcomes**

- if Gradle or repository downloads fail, record exact failure point
- if runtime flow fails, separate code failure from environment failure
