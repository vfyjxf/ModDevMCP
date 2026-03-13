# Hover And Runtime Flow Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add GUI-coordinate hover input and a reusable real runtime flow runner that saves a screenshot after every step.

**Architecture:** `moddev.input_action` already routes through `MinecraftInputController` and `LiveClientInputBridge`, so hover should stay in that path as new actions. The real runtime flow should stay outside game code and reuse the generated `game MCP` bridge, recording `ui_get_live_screen`, action payloads, and screenshot outputs per step.

**Tech Stack:** Java 21, NeoForge, JUnit 5, PowerShell

---

### Task 1: Add failing tests for `move` and `hover`

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputControllerTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/InputToolProviderTest.java`

**Step 1: Write the failing tests**

- add one controller test for default GUI coordinates with `move`
- add one controller test for `hoverDelayMs`
- add one tool-level test that forwards `action = "hover"`

**Step 2: Run tests to verify failure**

Run: `.\gradlew.bat :Mod:test --tests "*MinecraftInputControllerTest" --tests "*InputToolProviderTest" --no-daemon`
Expected: fail because `move` and `hover` are not implemented yet

### Task 2: Implement hover input

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/InputCommand.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputController.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/LiveClientInputBridge.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/InputToolProvider.java`

**Step 1: Add minimal command shape**

- support `move`
- support `hover`
- default coordinate space to `gui`
- accept optional `hoverDelayMs`

**Step 2: Run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*MinecraftInputControllerTest" --tests "*InputToolProviderTest" --no-daemon`
Expected: pass

### Task 3: Add a real runtime flow runner

**Files:**
- Create: `tools/runtime/game-mcp-title-flow.ps1`
- Modify: `docs/plans/2026-03-11-hover-and-runtime-flow/checklist.md`
- Modify: `docs/plans/2026-03-11-hover-and-runtime-flow/impl.md`

**Step 1: Implement one reproducible title-screen flow**

- connect to `run-game-mcp-bridge.bat`
- save one pre-hover screenshot
- send one hover step
- save one post-hover screenshot
- write a small step log

**Step 2: Run the real flow**

Run: `powershell -ExecutionPolicy Bypass -File tools/runtime/game-mcp-title-flow.ps1`
Expected: screenshots and step log are written under a predictable output directory
