# Minecraft Command Support Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add side-aware MCP tools to list, suggest, and execute Minecraft server commands and NeoForge client commands.

**Architecture:** Introduce a small `runtime.command` model plus side-specific live command services, then expose them through a new `CommandToolProvider` registered on both client and server runtimes. Keep routing in the existing gateway by exposing the tools as aggregated `common` runtime tools.

**Tech Stack:** Java 21, NeoForge/Minecraft 1.21.1, Brigadier, JUnit 5, Gradle, Markdown docs

---

### Task 1: Add failing tests for command tool exposure

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/BuiltinProviderRegistrationTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/CommandToolProviderTest.java`
- Modify: `Server/src/test/java/dev/vfyjxf/mcp/server/protocol/McpProtocolRuntimeDispatchTest.java`

**Step 1: Write failing tests**

Cover:

- builtin runtime registration exposes `moddev.command_list`, `moddev.command_suggest`, `moddev.command_execute`
- command tool provider defines common schemas
- command execute delegates normalized command text to the service
- aggregated runtime tool listing shows command tools once across client/server

**Step 2: Run focused tests to verify they fail**

Run: `.\\gradlew.bat :Mod:test --tests "*CommandToolProviderTest" --tests "*BuiltinProviderRegistrationTest" --no-daemon --rerun-tasks`
Expected: FAIL because command provider and command model do not exist yet

### Task 2: Implement runtime command model and tool provider

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/command/*`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/CommandToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientRuntimeBootstrap.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ServerRuntimeBootstrap.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/host/GameRuntimeDescriptorFactory.java`

**Step 1: Add shared command DTOs and service interface**

Define list/suggest/execute requests and results with immutable payloads.

**Step 2: Add Brigadier-backed support logic**

Implement command listing, suggestion, normalization, origin inference, and structured execution capture.

**Step 3: Register tools on both runtimes**

Expose the three tools as common runtime tools and mark `command_execute` mutating.

**Step 4: Run focused tests**

Run: `.\\gradlew.bat :Mod:test --tests "*CommandToolProviderTest" --tests "*BuiltinProviderRegistrationTest" --tests "*McpProtocolRuntimeDispatchTest" --no-daemon --rerun-tasks`
Expected: PASS

### Task 3: Implement live client/server command services

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/command/LiveClientCommandService.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/command/LiveServerCommandService.java`

**Step 1: Implement client access**

Use `ClientCommandHandler` dispatcher/source on the client thread. Access the handler reflectively so registration tests do not fail in non-client environments.

**Step 2: Implement server access**

Use `MinecraftServer` dispatcher and elevated `CommandSourceStack` on the server thread.

**Step 3: Re-run focused tests**

Run: `.\\gradlew.bat :Mod:test --tests "*CommandToolProviderTest" --tests "*BuiltinProviderRegistrationTest" --no-daemon --rerun-tasks`
Expected: PASS

### Task 4: Write and normalize docs

**Files:**
- Create: `docs/plans/2026-03-15-minecraft-command-support/design.md`
- Create: `docs/plans/2026-03-15-minecraft-command-support/plan.md`
- Create: `docs/plans/2026-03-15-minecraft-command-support/checklist.md`
- Create: `docs/plans/2026-03-15-minecraft-command-support/impl.md`
- Modify: `README.md`
- Modify: `docs/guides/2026-03-11-game-mcp-guide.md`
- Create: `docs/guides/2026-03-15-command-tools-guide.md`

**Step 1: Record design and execution notes**

Keep the new plan folder aligned with current repo conventions.

**Step 2: Update user-facing docs**

Document the new command tools and recommended usage.

### Task 5: Verify end-to-end build and tests

**Files:**
- Modify: `docs/plans/2026-03-15-minecraft-command-support/checklist.md`
- Modify: `docs/plans/2026-03-15-minecraft-command-support/impl.md`

**Step 1: Run focused verification**

Run:

- `.\\gradlew.bat :Mod:test --tests "*CommandToolProviderTest" --tests "*BuiltinProviderRegistrationTest" --no-daemon --rerun-tasks`
- `.\\gradlew.bat :Server:test --tests "*McpProtocolRuntimeDispatchTest" --no-daemon --rerun-tasks`

Expected: both PASS

**Step 2: Run full repository verification**

Run: `.\\gradlew.bat test --no-daemon`
Expected: `BUILD SUCCESSFUL`

**Step 3: Record actual outputs**

Write real red/green and verification outcomes into `impl.md`.
