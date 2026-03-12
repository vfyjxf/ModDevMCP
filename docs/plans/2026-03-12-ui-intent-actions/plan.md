# UI Intent Actions Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** replace the misleading `moddev.ui_open` tool with an explicit `moddev.ui_run_intent` tool for supported high-level UI entry actions.

**Architecture:** keep same-screen automation on `ui_click_ref`, `ui_hover_ref`, `ui_switch`, and `ui_batch`, but move high-level entry actions to a dedicated intent tool. `UiToolProvider` owns the MCP surface, while runtime-specific execution stays honest: vanilla maps supported intents to real triggers, fallback returns `unsupported_intent`.

**Tech Stack:** Java 21, NeoForge, Gson-backed MCP runtime, JUnit 5, Gradle

---

### Task 1: Replace registry and schema coverage

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolProviderTest.java`
- Modify: `README.md`
- Modify: `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`

**Step 1: Write the failing test**

- remove assertions that require `moddev.ui_open`
- add assertions that require `moddev.ui_run_intent`
- assert its input schema includes `intent`
- assert its output schema includes `intent` and `performed`

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolProviderTest" --no-daemon`
Expected: FAIL because production code still registers `ui_open` and not `ui_run_intent`

**Step 3: Update minimal docs text**

- replace active references that recommend `ui_open`
- document `ui_run_intent` as the explicit high-level entry tool

**Step 4: Re-run the focused test**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolProviderTest" --no-daemon`
Expected: still FAIL until code is implemented

### Task 2: Replace tool invocation behavior

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/FallbackRegionUiDriver.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaScreenUiDriver.java`

**Step 1: Write the failing tests**

- replace `uiOpenDelegatesSemanticOpenAction`
- add `uiRunIntentReturnsUnsupportedIntentForFallbackDriver`
- keep `uiClose...` and `uiSwitch...` coverage unchanged
- add one test that `ui_run_intent` returns `unsupported_intent` for an unknown intent

**Step 2: Run focused tests to verify failure**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because `ui_run_intent` does not exist yet

**Step 3: Write minimal implementation**

- remove `moddev.ui_open` registration and definition
- add `moddev.ui_run_intent`
- keep `ui_close` and `ui_switch`
- for unsupported drivers or unknown intents, return `unsupported_intent`
- do not fake success in fallback driver

**Step 4: Re-run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS for new intent behavior and existing close/switch behavior

### Task 3: Wire real vanilla intent execution

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputController.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/InputCommand.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/InputToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/InputToolProviderTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing tests**

- add a focused path for intent-to-input mapping
- cover at least one supported intent routed through existing input mechanisms

**Step 2: Run focused tests to verify failure**

Run: `.\gradlew.bat :Mod:test --tests "*InputToolProviderTest" --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because supported intents are not wired to real runtime triggers

**Step 3: Write minimal implementation**

- support `inventory`
- support `chat`
- support `pause_menu`
- map these to existing runtime input execution paths instead of new fake session state toggles

**Step 4: Re-run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*InputToolProviderTest" --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS

### Task 4: Update active docs and historical references

**Files:**
- Modify: `README.md`
- Modify: `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- Modify: `docs/plans/2026-03-09-moddev-mcp-framework/design.md`
- Modify: `docs/plans/2026-03-11-ui-live-screen-defaults/design.md`
- Add: `docs/plans/2026-03-12-ui-intent-actions/checklist.md`
- Add: `docs/plans/2026-03-12-ui-intent-actions/impl.md`

**Step 1: Update docs**

- remove active guidance that treats `ui_open` as a recommended tool
- add `ui_run_intent` usage guidance
- keep older historical text only where it is explicitly marked as historical

**Step 2: Write checklist and impl scaffolding**

- create checklist entries for implementation and verification
- create impl doc stub with date and pending status

**Step 3: Run doc-related focused test**

Run: `.\gradlew.bat :Mod:test --tests "*EmbeddedModDevMcpStdioMainTest" --no-daemon`
Expected: PASS

### Task 5: Run real runtime verification

**Files:**
- Verify only

**Step 1: Start or reuse real client**

Run: `cd TestMod; .\gradlew.bat runClient --no-daemon`
Expected: game starts and bridge is reachable

**Step 2: Verify supported intent on live runtime**

Run a real bridge probe using `moddev.ui_run_intent` plus `moddev.ui_get_live_screen`

Expected:

- supported intent produces a real visible state transition
- unsupported intent returns `unsupported_intent`

**Step 3: Run broad verification**

Run:
- `.\gradlew.bat :Server:test :Mod:test --no-daemon`
- `cd TestMod; .\gradlew.bat compileJava --no-daemon`

Expected: PASS

**Step 4: Record exact results**

- write exact commands
- write real screen transitions
- distinguish code failures from Gradle or dependency environment failures
