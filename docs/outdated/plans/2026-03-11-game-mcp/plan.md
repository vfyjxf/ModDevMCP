# Game MCP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the stable-server/backend startup chain with a game MCP endpoint started by the Minecraft client process.

**Architecture:** The game process will directly host MCP using existing `Server` transport code. External agents connect only after the game is already running, through a minimal connect-only bridge or documented local port. Stable-server startup, backend session management, and related config/install flows are removed from the primary path.

**Tech Stack:** Java 21, NeoForge, Gradle, MCP SDK, Gson

---

### Task 1: Add plan/checklist/impl docs for the simplified architecture

**Files:**
- Create: `docs/plans/2026-03-11-game-mcp/design.md`
- Create: `docs/plans/2026-03-11-game-mcp/plan.md`
- Create: `docs/plans/2026-03-11-game-mcp/checklist.md`
- Create: `docs/plans/2026-03-11-game-mcp/impl.md`

**Step 1: Write the docs**

- describe the new startup model
- explicitly mark stable/backend flow as removed from the primary path

**Step 2: Verify files exist**

Run: `Get-ChildItem docs\\plans\\2026-03-11-game-mcp*`
Expected: all new plan files exist

### Task 2: Replace runtime startup with a game MCP server

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/bootstrap/*`
- Create or modify: game-host bootstrap classes under `Mod/src/main/java/dev/vfyjxf/mcp/bootstrap/`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/*`

**Step 1: Write/adjust failing tests for the new bootstrap shape**

- assert game startup registers providers and starts a game MCP host
- assert no stable/backend bootstrap dependency remains on the main path

**Step 2: Run tests and confirm failure**

Run: `.\gradlew.bat :Mod:test --tests "*Bootstrap*" --no-daemon`
Expected: fail until bootstrap is updated

**Step 3: Implement the new game-host bootstrap**

- start an in-process MCP socket host from the game
- remove stable-server auto-start from the main path

**Step 4: Re-run bootstrap tests**

Run: `.\gradlew.bat :Mod:test --tests "*Bootstrap*" --no-daemon`
Expected: pass

### Task 3: Add a minimal connect-only bridge and normalize naming

**Files:**
- Modify: bridge/bootstrap classes in `Mod/src/main/java/dev/vfyjxf/mcp/bootstrap/`
- Modify: `Server` transport helpers only if needed
- Test: matching bootstrap/transport tests

**Step 1: Add tests for connect-only behavior**

- bridge should only connect to an existing game MCP socket
- it must not auto-start game or stable-server infrastructure

**Step 2: Implement renamed/simplified bridge entrypoint**

- remove “stable” semantics from names and docs for the primary path

**Step 3: Run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*Bridge*" --tests "*Bootstrap*" --no-daemon`
Expected: pass

### Task 4: Clean TestMod and Gradle wiring

**Files:**
- Modify: `TestMod/build.gradle`
- Modify: `Mod/build.gradle`
- Modify: any generated launch/install tasks that still assume stable-server-first

**Step 1: Remove stable install dependency from the normal TestMod run path**

- `runClient` should no longer depend on `:Server:installLocalStableServer`

**Step 2: Add minimal game-host MCP config if needed**

- fixed localhost port only

**Step 3: Verify Gradle tasks**

Run: `cd TestMod; .\gradlew.bat tasks --all --no-daemon`
Expected: `runClient` still exists and no longer documents stable-server dependency as the normal path

### Task 5: Update README and guides

**Files:**
- Modify: `README.md`
- Modify: `docs/guides/*agent*`
- Modify: `docs/guides/*testmod*`
- Create: `docs/guides/2026-03-11-game-mcp-guide.md`

**Step 1: Replace stable-server-first instructions**

- start game first
- then connect MCP client
- then check connection/status using the prompt pattern

**Step 2: Preserve prompt guidance**

- agent must verify connection before using tools

**Step 3: Verify docs by searching for old primary-path wording**

Run: `rg -n "stable server|stable-server-first|installLocalStableServer" README.md docs TestMod/build.gradle`
Expected: remaining references are only historical plan docs or explicitly marked legacy notes

### Task 6: Real verification

**Files:**
- Use existing runtime logs under `TestMod/build/demo/`
- Record results in `docs/plans/2026-03-11-game-mcp/impl.md`

**Step 1: Run focused automated tests**

Run: `.\gradlew.bat :Mod:test :Server:test --no-daemon`
Expected: pass, or report exact failing modules

**Step 2: Run real client**

Run: `cd TestMod; .\gradlew.bat runClient --no-daemon`
Expected: game starts and hosts MCP directly

**Step 3: Verify MCP behavior**

- connect through the simplified bridge/client path
- call at least:
  - `moddev.ui_get_live_screen`
  - `moddev.ui_capture`

**Step 4: Record real result**

- write exact pass/fail outcomes
- separate code failures from network/TLS/repository failures
