# Local World Tools Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add client-runtime MCP tools to list local worlds, create a new local world, and enter an existing local world through the real Minecraft client.

**Architecture:** Introduce a `runtime.world` service layer for local world listing/creation/joining, then expose it through a new `WorldToolProvider` registered only on the client runtime. Use native client world APIs instead of pure UI clicking, but require real client window state transitions as the observable outcome.

**Tech Stack:** Java 21, NeoForge/Minecraft 1.21.1 client APIs, JUnit 5, Gradle, Markdown docs

---

### Task 1: Add failing tests for world tool exposure

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/BuiltinProviderRegistrationTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/WorldToolProviderTest.java`

**Step 1: Write the failing tests**

Cover:

- client runtime registration exposes `moddev.world_list`, `moddev.world_create`, `moddev.world_join`
- world tools are marked `client`
- provider schemas expose the agreed fields and outputs
- `world_join` prefers `id` over `name`
- `world_create` defaults `joinAfterCreate=true`

**Step 2: Run tests to verify they fail**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.WorldToolProviderTest --tests dev.vfyjxf.mcp.runtime.tool.BuiltinProviderRegistrationTest --no-daemon --rerun-tasks`
Expected: FAIL because world provider/service do not exist yet

### Task 2: Add runtime world model and tool provider

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/world/WorldDescriptor.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/world/WorldListResult.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/world/WorldCreateRequest.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/world/WorldCreateResult.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/world/WorldJoinRequest.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/world/WorldJoinResult.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/world/WorldService.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/world/WorldServiceException.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/WorldToolProvider.java`

**Step 1: Write minimal immutable DTOs and service contract**

Keep the request/result types small and match the approved MCP shape.

**Step 2: Implement provider against a fake service**

Handle default values, `id` vs `name` precedence, payload formatting, and structured failures.

**Step 3: Run focused tests**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.WorldToolProviderTest --no-daemon --rerun-tasks`
Expected: PASS

### Task 3: Implement live client world service

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/world/LiveClientWorldService.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientRuntimeBootstrap.java`

**Step 1: Implement world listing**

Read local world metadata through client save list APIs and map it into `WorldDescriptor`.

**Step 2: Implement world create**

Create a world with minimal settings:

- `name`
- `gameMode`
- `allowCheats`
- `seed`

Default `joinAfterCreate=true` and ensure the real client transitions into the world when joining.

**Step 3: Implement world join**

Resolve the target world by `id` or `name`, then enter it through the client’s native singleplayer join flow.

**Step 4: Register provider**

Add `WorldToolProvider` to `ClientRuntimeBootstrap`.

**Step 5: Re-run focused tests**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.WorldToolProviderTest --tests dev.vfyjxf.mcp.runtime.tool.BuiltinProviderRegistrationTest --no-daemon --rerun-tasks`
Expected: PASS

### Task 4: Write docs

**Files:**
- Create: `docs/plans/2026-03-15-local-world-tools/checklist.md`
- Create: `docs/plans/2026-03-15-local-world-tools/impl.md`
- Modify: `README.md`
- Modify: `docs/guides/2026-03-11-game-mcp-guide.md`
- Create: `docs/guides/2026-03-15-local-world-tools-guide.md`

**Step 1: Document design and implementation**

Keep the plan folder aligned with the repo convention.

**Step 2: Update user-facing docs**

Document local-world tool purpose, limits, and example calls.

### Task 5: Verify

**Files:**
- Modify: `docs/plans/2026-03-15-local-world-tools/checklist.md`
- Modify: `docs/plans/2026-03-15-local-world-tools/impl.md`

**Step 1: Run focused verification**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.WorldToolProviderTest --tests dev.vfyjxf.mcp.runtime.tool.BuiltinProviderRegistrationTest --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`

**Step 2: Run compile verification**

Run: `gradlew.bat :Mod:compileJava --no-daemon`
Expected: `BUILD SUCCESSFUL`

**Step 3: Record actual results**

Write the real commands and outcomes into `impl.md`.
