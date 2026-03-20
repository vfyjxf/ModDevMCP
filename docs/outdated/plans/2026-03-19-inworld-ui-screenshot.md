# In-World UI Screenshot Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make `ui.screenshot` succeed in-world when `screenClass` is blank and a live capture-capable driver is available.

**Architecture:** Keep the existing live capture flow intact and narrow the change to the screenshot precondition. Preserve stricter screen requirements for session refresh and other UI operations.

**Tech Stack:** Java, JUnit 5, NeoForge runtime abstractions, ModDevMCP HTTP operation bindings

---

### Task 1: Add the failing screenshot regression test

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing test**

Add a test that registers a real offscreen capture provider, exposes in-world metrics with blank `screenClass`, calls `moddev.ui_screenshot`, and expects success with a concise payload.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*UiToolInvocationTest.uiScreenshotSupportsInWorldLiveFlowWithoutActiveScreen" --no-daemon`
Expected: FAIL because the current implementation returns `screen_unavailable`.

### Task 2: Implement the minimal screenshot precondition change

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`

**Step 1: Write minimal implementation**

Change the live screenshot path so blank `screenClass` is accepted when live metrics indicate `inWorld=true`.

**Step 2: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*UiToolInvocationTest.uiScreenshotSupportsInWorldLiveFlowWithoutActiveScreen" --no-daemon`
Expected: PASS.

### Task 3: Run focused regression coverage

**Files:**
- Modify: none unless a regression appears

**Step 1: Run relevant tests**

Run: `./gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --tests "*RuntimeOperationBindingsTest" --no-daemon`
Expected: PASS.

### Task 4: Verify through the HTTP service

**Files:**
- Modify: none

**Step 1: Run live request**

Issue `status.live_screen` and `ui.screenshot` against the running client while in-world and confirm the operation returns an `imagePath` instead of `screen_unavailable`.