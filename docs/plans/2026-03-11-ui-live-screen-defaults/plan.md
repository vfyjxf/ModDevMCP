# UI Live Screen Defaults Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the UI tool family default to the current live screen when `screenClass` and `modId` are omitted.

**Architecture:** `UiToolProvider` already owns UI tool argument normalization and already has a `ClientScreenProbe`. The change should stay local to that provider: add a small argument-to-`UiContext` resolution step that merges explicit arguments with live screen metrics, then reuse it across all `ui_*` handlers.

**Tech Stack:** Java 21, NeoForge, JUnit 5

---

### Task 1: Add failing tests for omitted screen arguments

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing tests**

- add a snapshot case that omits `screenClass`
- add a capture case that omits `screenClass`
- add an action case that omits `screenClass`

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: new tests fail because `UiToolProvider` still defaults to `custom.UnknownScreen`

### Task 2: Resolve UI context from live screen metrics

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Implement the minimal merge logic**

- if `screenClass` is blank or absent, use `screenProbe.metrics().screenClass()`
- if `modId` is blank or absent, keep explicit value first, otherwise use live-screen-derived default
- keep explicit values untouched

**Step 2: Re-run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: pass

### Task 3: Verify docs and real usage wording if needed

**Files:**
- Modify: `docs/plans/2026-03-11-ui-live-screen-defaults/checklist.md`
- Modify: `docs/plans/2026-03-11-ui-live-screen-defaults/impl.md`

**Step 1: Record focused verification**

- write the exact command used
- record the exact outcome

**Step 2: Record runtime expectation**

- `ui_capture` should succeed after `ui_get_live_screen` even when the caller omits `screenClass`
