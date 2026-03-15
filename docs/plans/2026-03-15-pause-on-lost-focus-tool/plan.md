# Pause On Lost Focus Tool Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a client-runtime MCP tool that can query and set Minecraft's pause-on-lost-focus option and persist the change.

**Architecture:** Introduce a small client-side service that reads and writes the `Options.pauseOnLostFocus` setting on the client thread, then expose it through a dedicated `moddev.pause_on_lost_focus` tool. Register the tool only on the client runtime and keep the schema narrow: optional `enabled` input and `enabled/changed` output.

**Tech Stack:** Java 21, NeoForge/Minecraft 1.21.1 client APIs, JUnit 5, Gradle, Markdown docs

---

### Task 1: Add failing tests for tool exposure and schema

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/BuiltinProviderRegistrationTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/PauseOnLostFocusToolProviderTest.java`

**Step 1: Write the failing tests**

Cover:

- client runtime registration exposes `moddev.pause_on_lost_focus`
- tool side is `client`
- input schema exposes optional `enabled`
- output schema exposes `enabled` and `changed`
- query call reports current state without changing it
- set call updates the state and reports whether it changed

**Step 2: Run tests to verify they fail**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.PauseOnLostFocusToolProviderTest --tests dev.vfyjxf.mcp.runtime.tool.BuiltinProviderRegistrationTest --no-daemon --rerun-tasks`
Expected: FAIL because the tool/provider do not exist yet

### Task 2: Implement the client-side service and tool provider

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/game/PauseOnLostFocusService.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/game/LiveClientPauseOnLostFocusService.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/PauseOnLostFocusToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientRuntimeBootstrap.java`

**Step 1: Implement the service**

Read and write `Minecraft.getInstance().options.pauseOnLostFocus` on the client thread and call `options.save()` after updates.

**Step 2: Implement the tool provider**

Support:

- query when `enabled` is absent
- set when `enabled` is present
- payload with `runtimeId`, `runtimeSide`, `enabled`, `changed`

**Step 3: Register provider**

Add the new provider to `ClientRuntimeBootstrap`.

**Step 4: Run focused tests**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.PauseOnLostFocusToolProviderTest --tests dev.vfyjxf.mcp.runtime.tool.BuiltinProviderRegistrationTest --no-daemon --rerun-tasks`
Expected: PASS

### Task 3: Update docs and usage skill

**Files:**
- Create: `docs/plans/2026-03-15-pause-on-lost-focus-tool/impl.md`
- Create: `docs/plans/2026-03-15-pause-on-lost-focus-tool/checklist.md`
- Modify: `README.md`
- Modify: `docs/guides/2026-03-11-game-mcp-guide.md`
- Modify: `skills/moddevmcp-usage/SKILL.md`

**Step 1: Document the tool**

Describe query/set behavior, persistence, and a minimal example call.

**Step 2: Record real verification output**

Write the actual commands and results into `impl.md`.

### Task 4: Verify

**Files:**
- Modify: `docs/plans/2026-03-15-pause-on-lost-focus-tool/impl.md`
- Modify: `docs/plans/2026-03-15-pause-on-lost-focus-tool/checklist.md`

**Step 1: Run focused verification**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.PauseOnLostFocusToolProviderTest --tests dev.vfyjxf.mcp.runtime.tool.BuiltinProviderRegistrationTest --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`

**Step 2: Run compile verification**

Run: `gradlew.bat :Mod:compileJava --no-daemon`
Expected: `BUILD SUCCESSFUL`

**Step 3: Record actual results**

Write the real commands and outcomes into `impl.md`.
