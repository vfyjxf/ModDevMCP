# Game Close Tool Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rework `moddev.game_close` into a common tool that can target both client and server runtimes through a multi-runtime gateway.

**Architecture:** First refactor the gateway from single active runtime to multi-runtime tracking and side-aware routing. Then register `moddev.game_close` on both Mod client and Mod server runtimes using side-specific closers, while exposing one aggregated common tool to MCP agents.

**Tech Stack:** Java 21, NeoForge/Minecraft 1.21.1, JUnit 5, Gradle, Markdown docs

---

### Task 1: Update design docs for multi-runtime routing

**Files:**
- Modify: `docs/plans/2026-03-14-game-close-tool/design.md`
- Modify: `docs/plans/2026-03-14-game-close-tool/plan.md`
- Modify: `docs/plans/2026-03-14-game-close-tool/checklist.md`
- Modify: `docs/plans/2026-03-14-game-close-tool/impl.md`

**Step 1: Rewrite docs**

Document that the tool is now common, targetable by side, and depends on a multi-runtime gateway.

**Step 2: Verify docs exist**

Run: `Get-ChildItem docs\plans\2026-03-14-game-close-tool`
Expected: the plan folder still contains all four docs

### Task 2: Write failing Server tests for multi-runtime gateway behavior

**Files:**
- Modify: `Server/src/test/java/dev/vfyjxf/mcp/server/host/RuntimeRegistryTest.java`
- Modify: `Server/src/test/java/dev/vfyjxf/mcp/server/host/HostStatusToolProviderTest.java`
- Modify: `Server/src/test/java/dev/vfyjxf/mcp/server/protocol/McpProtocolRuntimeDispatchTest.java`
- Modify as needed: `Server/src/test/java/dev/vfyjxf/mcp/server/host/transport/RuntimeHostCallDispatchTest.java`

**Step 1: Write failing tests**

Cover:
- client and server runtimes can coexist
- same-name dynamic tool can be resolved by `targetSide`
- missing `targetSide` with both sides connected yields `ambiguous_runtime_side`
- status payload exposes client/server connection flags

**Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :Server:test --tests "*RuntimeRegistryTest" --tests "*HostStatusToolProviderTest" --tests "*McpProtocolRuntimeDispatchTest" --no-daemon --rerun-tasks`
Expected: FAIL because gateway is still single-runtime

### Task 3: Implement multi-runtime registry and side-aware dispatch

**Files:**
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/host/RuntimeRegistry.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/host/RuntimeState.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/host/HostStatusToolProvider.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/host/RuntimeCallQueue.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/host/transport/RuntimeHost.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcher.java`
- Modify as needed: dispatcher / descriptor helper classes

**Step 1: Add multi-runtime session storage**

Allow separate client and server sessions to be tracked simultaneously.

**Step 2: Add side-aware dynamic tool resolution**

Support `targetSide`, single-side auto selection, and ambiguity errors.

**Step 3: Run focused Server tests**

Run: `.\gradlew.bat :Server:test --tests "*RuntimeRegistryTest" --tests "*HostStatusToolProviderTest" --tests "*McpProtocolRuntimeDispatchTest" --no-daemon --rerun-tasks`
Expected: PASS

### Task 4: Write failing Mod tests for dual-side game close

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/BuiltinProviderRegistrationTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/GameToolProviderTest.java`
- Modify or add runtime host bootstrap tests under `Mod/src/test/java/dev/vfyjxf/mcp/runtime/host/`

**Step 1: Write failing tests**

Cover:
- `moddev.game_close` is exposed as `common`
- client runtime uses client closer
- server runtime uses server closer
- server runtime host hello advertises `supportedSides=["server"]`

**Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :Mod:test --tests "*BuiltinProviderRegistrationTest" --tests "*GameToolProviderTest" --tests "*HostGameClientTest" --no-daemon --rerun-tasks`
Expected: FAIL because only client path exists

### Task 5: Implement dual-side game close and server runtime connection

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ServerEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Create or modify: runtime bootstrap classes for server side
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/host/HostGameClient.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/GameToolProvider.java`
- Modify / create: game closer interfaces and live implementations

**Step 1: Add server runtime bootstrap and host reconnect**

Make dedicated server connect to gateway just like client currently does.

**Step 2: Register `moddev.game_close` on both sides**

Expose one common tool with side-specific runtime behavior.

**Step 3: Run focused Mod tests**

Run: `.\gradlew.bat :Mod:test --tests "*BuiltinProviderRegistrationTest" --tests "*GameToolProviderTest" --tests "*HostGameClientTest" --no-daemon --rerun-tasks`
Expected: PASS

### Task 6: Run full verification and record real results

**Files:**
- Modify: `docs/plans/2026-03-14-game-close-tool/checklist.md`
- Modify: `docs/plans/2026-03-14-game-close-tool/impl.md`

**Step 1: Run full repository tests**

Run: `.\gradlew.bat test --no-daemon`
Expected: `BUILD SUCCESSFUL`

**Step 2: Record actual outputs**

Update impl notes with real red/green commands and outcomes.
