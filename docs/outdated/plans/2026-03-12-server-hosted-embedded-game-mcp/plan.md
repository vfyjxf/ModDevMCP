# Server-Hosted Embedded Game MCP Refactor Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** move embedded game-MCP server bootstrap/runtime code back into `Server`, so `Mod` only owns Minecraft runtime/provider wiring and calls the server-side bootstrap.

**Architecture:** keep the deployed shape unchanged: Minecraft still hosts MCP in-process on `127.0.0.1:47653` by default. Refactor only the code boundary: `Server` owns embedded host/bootstrap/runtime classes and transport startup, while `Mod` owns runtime registries, tool/resource providers, and the call site that starts/stops the embedded server.

**Tech Stack:** Java 21, Gradle multi-project build, NeoForge ModDevGradle, MCP Java SDK core, Gson, JUnit 5.

---

### Task 1: Introduce a server-side embedded socket runtime bootstrap

**Files:**
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/EmbeddedGameMcpConfig.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/EmbeddedGameMcpRuntime.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpServerFactory.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/transport/SocketMcpServerHost.java`
- Create: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/EmbeddedGameMcpRuntimeTest.java`

**Step 1: Write/adjust the failing server-side test**

Cover:
- a prepared `ModDevMcpServer` can be started through a server-owned embedded runtime
- the runtime binds the configured port
- the runtime can be closed cleanly

**Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat :Server:test --tests "*EmbeddedGameMcpRuntimeTest" --no-daemon`

Expected: fail because the embedded server-owned runtime class does not exist yet.

**Step 3: Implement the minimal server bootstrap**

Add:
- `EmbeddedGameMcpConfig` as the shared fixed host/port config loader
- `EmbeddedGameMcpRuntime.start(ModDevMcpServer, EmbeddedGameMcpConfig)`
- keep the actual listening transport in `Server`

**Step 4: Re-run the test**

Run: `.\gradlew.bat :Server:test --tests "*EmbeddedGameMcpRuntimeTest" --no-daemon`

Expected: pass.

### Task 2: Move mod-side game runtime wrapper to the new server bootstrap

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/bootstrap/GameMcpRuntime.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/bootstrap/GameMcpConfig.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/GameMcpRuntimeTest.java`

**Step 1: Rewrite the mod-side call site**

Change the client entrypoint to start the embedded MCP through the new `Server` bootstrap.

**Step 2: Rewrite the focused runtime test**

Keep the behavior assertion:
- game-hosted socket MCP still serves built-in tools

But move the test to the new server-owned runtime API.

**Step 3: Run focused mod/server verification**

Run: `.\gradlew.bat :Server:test --tests "*EmbeddedGameMcpRuntimeTest" :Mod:test --tests "*GameMcpRuntimeTest" --no-daemon`

Expected: pass.

### Task 3: Move embedded stdio bootstrap ownership to `Server`

**Files:**
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/EmbeddedModDevMcpHost.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/bootstrap/EmbeddedModDevMcpStdioMain.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/EmbeddedModDevMcpStdioMainTest.java`

**Step 1: Introduce a server-owned helper for embedded host creation**

This helper should accept a prepared `ModDevMcpServer` and return the stdio host from `Server`.

**Step 2: Collapse the mod-side stdio bootstrap to a thin delegating shell**

`Mod` should only:
- instantiate `ModDevMCP`
- register providers
- pass the prepared server to the `Server` helper

**Step 3: Run focused verification**

Run: `.\gradlew.bat :Mod:test --tests "*EmbeddedModDevMcpStdioMainTest" :Server:test --tests "*ModDevMcpStdioMainTest" --no-daemon`

Expected: pass.

### Task 4: Clean up duplicated/aged bootstrap code and naming

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpStdioMain.java`
- Modify: `README.md`
- Modify: `docs/guides/2026-03-11-game-mcp-guide.md`
- Modify: `docs/plans/2026-03-12-embedded-game-mcp-cleanup/impl.md`
- Add: `docs/plans/2026-03-12-server-hosted-embedded-game-mcp/impl.md`
- Add: `docs/plans/2026-03-12-server-hosted-embedded-game-mcp/checklist.md`

**Step 1: Remove outdated wording**

Clarify:
- embedded runtime is hosted by the game process
- bootstrap implementation ownership is now back in `Server`
- `Mod` is the runtime/provider side

**Step 2: Simplify any redundant helper names/fields**

Remove wrappers that no longer add value after the bootstrap move.

### Task 5: Run verification and record exact results

**Files:**
- Verify: `Server/src/main/java/dev/vfyjxf/mcp/server/**`
- Verify: `Mod/src/main/java/dev/vfyjxf/mcp/**`
- Verify: `README.md`
- Verify: `docs/guides/**`

**Step 1: Run focused verification**

Run:
- `.\gradlew.bat :Server:test --tests "*EmbeddedGameMcpRuntimeTest" --tests "*ModDevMcpServerFactoryTest" --tests "*McpProtocolDispatcherTest" --no-daemon`
- `.\gradlew.bat :Mod:test --tests "*GameMcpRuntimeTest" --tests "*EmbeddedModDevMcpStdioMainTest" --tests "*BuiltinProviderRegistrationTest" --no-daemon`

**Step 2: Run broad verification**

Run:
- `.\gradlew.bat :Server:test :Mod:test --no-daemon`
- `.\TestMod\gradlew.bat compileJava --no-daemon`

**Step 3: Report blockers exactly**

If dependency download / TLS / repository failures happen, record them as environment issues.
If compile/test failures happen after resolution, record them as code issues.
