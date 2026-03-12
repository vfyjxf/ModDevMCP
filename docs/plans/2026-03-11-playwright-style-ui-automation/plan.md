# Playwright-Style UI Automation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a Playwright-style thin UI automation layer with session refs, batched actions, and structured trace on top of the existing `moddev` runtime tools.

**Architecture:** Keep the current low-level `ui_*` and `input_action` tools as the capability layer, then add a small in-process automation layer in `Mod` that manages sessions, resolves opaque refs to current targets, executes ordered step batches, and records trace entries. The new MCP surface should be exposed from `UiToolProvider` so game-side behavior remains local to the running client and no extra backend lifecycle is introduced.

**Tech Stack:** Java 21, NeoForge, Gson-backed MCP runtime, JUnit 5, Gradle

---

### Task 1: Add failing tests for session lifecycle and ref resolution

**Files:**
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManagerTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolProviderTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing tests**

- add one test that opens a session and returns `sessionId` plus non-empty refs
- add one test that refreshes a session and marks a screen change
- add one test that resolves a ref on the same screen
- add one test that returns `target_stale` when the screen changes and the old target disappears

**Step 2: Run tests to verify failure**

Run: `.\gradlew.bat :Mod:test --tests "*UiAutomationSessionManagerTest" --tests "*UiToolProviderTest" --tests "*UiToolInvocationTest" --no-daemon`
Expected: fail because session storage, refs, and new tool definitions do not exist yet

**Step 3: Write minimal implementation**

- create a small session manager
- store latest snapshot per session
- generate opaque refs mapped to current target ids
- add minimal lookup and refresh APIs

**Step 4: Run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*UiAutomationSessionManagerTest" --tests "*UiToolProviderTest" --tests "*UiToolInvocationTest" --no-daemon`
Expected: session-focused tests pass, batch and trace tests still absent

### Task 2: Add thin single-step automation tools

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManager.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSession.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationRef.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java`

**Step 1: Add new tool definitions**

- register `moddev.ui_session_open`
- register `moddev.ui_session_refresh`
- register `moddev.ui_click_ref`
- register `moddev.ui_hover_ref`
- register `moddev.ui_press_key`
- register `moddev.ui_type_text`
- register `moddev.ui_wait_for`
- register `moddev.ui_screenshot`

**Step 2: Implement thin wrappers**

- `ui_session_open` should probe the live screen, take a snapshot, create a session, and return refs
- `ui_click_ref` and `ui_hover_ref` should resolve refs and call the existing UI action path
- `ui_press_key` and `ui_type_text` should route through the existing input controller path
- `ui_wait_for` should wrap the current wait capability with session-aware target resolution
- `ui_screenshot` should reuse current capture logic and fail with `capture_unavailable` when no real provider is available

**Step 3: Add or update tests**

- add tool registration assertions
- add invocation assertions for successful click and hover by ref
- add failure assertions for `screen_unavailable`, `session_not_found`, and `target_not_found`

**Step 4: Run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*UiAutomationSessionManagerTest" --tests "*UiToolProviderTest" --tests "*UiToolInvocationTest" --no-daemon`
Expected: pass for session and thin tool flows

### Task 3: Add failing tests for batch execution and trace

**Files:**
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationBatchTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationTraceTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing tests**

- add one batch test with `clickRef -> waitFor -> screenshot`
- add one stop-on-error test that fails at step index 1 and stops
- add one trace test that records elapsed time, action kind, and error code

**Step 2: Run tests to verify failure**

Run: `.\gradlew.bat :Mod:test --tests "*UiAutomationBatchTest" --tests "*UiAutomationTraceTest" --tests "*UiToolInvocationTest" --no-daemon`
Expected: fail because batch and trace support do not exist yet

**Step 3: Write minimal implementation**

- add batch step parsing and sequential execution
- add per-step result payloads
- add trace entry model and session-owned trace storage
- expose `moddev.ui_batch` and `moddev.ui_trace_get`

**Step 4: Run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*UiAutomationBatchTest" --tests "*UiAutomationTraceTest" --tests "*UiToolInvocationTest" --no-daemon`
Expected: pass for batch sequencing and trace coverage

### Task 4: Harden error codes and session refresh behavior

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManager.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManagerTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationBatchTest.java`

**Step 1: Normalize structured failures**

- map runtime probe failure to `runtime_unavailable`
- map inactive screen to `screen_unavailable`
- map unknown session ids to `session_not_found`
- map stale sessions or stale refs to `session_stale` or `target_stale`
- map batch failures to `batch_step_failed`

**Step 2: Add refresh-on-demand rules**

- refresh the session snapshot before ref-based actions when required
- keep same-screen actions cheap
- avoid forced screenshots during normal actions

**Step 3: Run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*UiAutomationSessionManagerTest" --tests "*UiAutomationBatchTest" --tests "*UiToolInvocationTest" --no-daemon`
Expected: pass with explicit error-code assertions

### Task 5: Real runtime verification and documentation

**Files:**
- Create: `docs/plans/2026-03-11-playwright-style-ui-automation/checklist.md`
- Create: `docs/plans/2026-03-11-playwright-style-ui-automation/impl.md`
- Modify: `README.md`
- Modify: `docs/guides/` (choose or create the most relevant installation/usage guide file after implementation context is clear)

**Step 1: Write the runtime verification checklist**

- document one real GUI flow that uses `ui_session_open`
- use `ui_batch` for one multi-step path
- save selected checkpoint screenshots
- dump trace output for inspection

**Step 2: Run real verification**

Run: `.\gradlew.bat :Mod:runClient --no-daemon`
Expected: game starts and MCP bridge is reachable

Run: real Codex MCP session against the new thin tools
Expected: one real GUI flow succeeds, screenshots are saved, trace output is returned, and no fallback capture is emitted on failure

**Step 3: Record real results**

- write the exact commands used
- write real screenshot paths
- write real trace excerpts
- clearly distinguish code failures from Gradle, network, TLS, or repository dependency failures if any occur
