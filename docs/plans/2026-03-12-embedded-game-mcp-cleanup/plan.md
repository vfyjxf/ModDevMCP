# Embedded Game MCP Cleanup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** remove the abandoned stable-server/backend/reconnect architecture and leave one primary runtime path: `runClient` starts Minecraft and the game-side MCP endpoint inside the same JVM.

**Architecture:** keep `Server` as the reusable MCP protocol/registry/transport library, but delete all stable-server-specific socket/backend/session/config/bootstrap code. Keep `Mod` as the only runtime integration path, update docs to the embedded game MCP workflow, and sanitize repo-specific absolute paths in user-facing examples.

**Tech Stack:** Java 21, Gradle, NeoForge ModDevGradle, MCP Java SDK core, Gson, JUnit 5.

---

### Task 1: Remove stable-server backend/runtime/bootstrap code from `Server`

**Files:**
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpServerFactory.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcher.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/backend/BackendRequestForwarder.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/backend/BackendSocketClientSession.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/backend/BackendSocketHost.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/SocketTransportProxy.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/StableMcpStdioBridgeMain.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/StableModDevMcpServerMain.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/StableServerLauncher.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/config/GameRuntimeLifecycleFiles.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/config/GameRuntimeLifecycleState.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/config/GameRuntimeLifecycleStatus.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/config/StableServerConfig.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/config/StableServerConfigFiles.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/runtime/BackendSessionManager.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/runtime/BackendSessionStatus.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/runtime/StableRuntimeStatusResolver.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/runtime/StableRuntimeStatusResourceProvider.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/runtime/StableRuntimeToolMetadataProvider.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/transport/SocketMcpServerHost.java`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/transport/StableServerSocketHost.java`

**Step 1: Remove backend-aware server factory branches**

Make `ModDevMcpServerFactory` expose only plain local server/stdio host creation and plain SDK tool conversion.

**Step 2: Remove dead stable-server classes**

Delete the stable/bootstrap/backend/config/runtime classes listed above.

**Step 3: Run focused compile/test verification**

Run: `.\gradlew.bat :Server:compileJava :Server:test --tests "*McpProtocolDispatcherTest" --tests "*ModDevMcpServerFactoryTest" --no-daemon`

Expected: pass after references are cleaned up.

### Task 2: Remove stable-server tests and references

**Files:**
- Delete: `Server/src/test/java/dev/vfyjxf/mcp/server/backend/BackendSocketHostTest.java`
- Delete: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/StableMcpStdioBridgeMainTest.java`
- Delete: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/StableServerLauncherTest.java`
- Delete: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/StableServerSocketBootstrapTest.java`
- Delete: `Server/src/test/java/dev/vfyjxf/mcp/server/config/StableServerConfigTest.java`
- Delete: `Server/src/test/java/dev/vfyjxf/mcp/server/protocol/BackendAwareMcpProtocolDispatcherTest.java`
- Delete: `Server/src/test/java/dev/vfyjxf/mcp/server/runtime/BackendSessionManagerTest.java`
- Modify: `Server/src/test/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcherTest.java`
- Modify: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpServerFactoryTest.java`

**Step 1: Delete tests that only exist for the removed architecture**

Remove backend/stable-server test files.

**Step 2: Rewrite remaining server tests to plain embedded semantics**

Keep protocol and SDK coverage, but remove assertions about backend-unavailable payloads, runtime status resources, or stable bootstrap.

**Step 3: Run focused server verification**

Run: `.\gradlew.bat :Server:test --no-daemon`

Expected: pass, or report exact remaining server-side failures.

### Task 3: Remove mod-side stable/reconnect leftovers and keep embedded runtime path

**Files:**
- Modify: `Mod/build.gradle`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/EmbeddedModDevMcpStdioMainTest.java`
- Delete any mod bootstrap/bridge test or code file that exists only for stable-server/backend startup

**Step 1: Remove stable-server launch/config generation and dead bootstrap references**

Keep only the embedded game MCP startup path used by `runClient`.

**Step 2: Keep bridge/config pieces only if they connect to the already running game MCP**

Delete anything that starts or proxies a stable server.

**Step 3: Run focused mod verification**

Run: `.\gradlew.bat :Mod:compileJava :Mod:test --tests "*EmbeddedModDevMcpStdioMainTest" --tests "*BuiltinProviderRegistrationTest" --tests "*UiToolInvocationTest" --no-daemon`

Expected: pass after mod-side references are updated.

### Task 4: Update docs, sanitize paths, and record the new primary workflow

**Files:**
- Modify: `README.md`
- Modify: `docs/guides/2026-03-11-game-mcp-guide.md`
- Modify: `docs/guides/2026-03-11-agent-prompt-templates.md`
- Modify: `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- Modify: `docs/plans/2026-03-12-playwright-style-ui-automation/checklist.md`
- Modify: `docs/plans/2026-03-12-playwright-style-ui-automation/impl.md`
- Add: `docs/plans/2026-03-12-embedded-game-mcp-cleanup/checklist.md`
- Add: `docs/plans/2026-03-12-embedded-game-mcp-cleanup/impl.md`

**Step 1: Rewrite user-facing docs to one path**

Document only:
- `TestMod\\gradlew.bat runClient --no-daemon`
- game-side MCP starts in the client JVM
- agents connect after the game is running

**Step 2: Remove or downgrade stable-server docs**

Mark old stable-server plans as historical context only by removing them from README/guides and not presenting them as current usage.

**Step 3: Sanitize absolute paths**

Replace repo-specific examples with relative paths or placeholders such as `<repo>\...`.

### Task 5: Format, run final verification, and record exact results

**Files:**
- Verify: `Server/src/main/java/dev/vfyjxf/mcp/server/**`
- Verify: `Mod/src/main/java/dev/vfyjxf/mcp/**`
- Verify: `README.md`
- Verify: `docs/guides/**`
- Verify: `docs/plans/2026-03-12-embedded-game-mcp-cleanup/**`

**Step 1: Run formatting/cleanup-safe tasks**

Run any existing Gradle formatting or style task if present; if none exists, limit cleanup to import/order and dead-code removal already touched by compilation.

**Step 2: Run end-to-end verification**

Run:
- `.\gradlew.bat :Server:test :Mod:test --no-daemon`
- `.\TestMod\gradlew.bat compileJava --no-daemon`

**Step 3: Report exact blockers separately**

If Gradle dependency/TLS/repository/network failures happen, report them as environment issues. If compile/test failures happen after dependency resolution, report them as code issues.
