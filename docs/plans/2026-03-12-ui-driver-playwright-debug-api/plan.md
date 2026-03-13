# UI Driver Playwright-Debug API Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Strengthen the low-level UI driver API with reusable default semantics for resolution, actionability, inspect, and waiting so higher-level Playwright-style debugging tools can be added cleanly.

**Architecture:** Keep `UiDriver` as the runtime-facing abstraction, but add stronger request/result models and default support methods. Move shared semantics out of ad hoc tool-layer logic and into reusable driver support, then build concise high-level MCP tools on top.

**Tech Stack:** Java 21, NeoForge client runtime, Gradle, JUnit 5

---

### Task 1: Add shared driver request/result models

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiTargetReference.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiLocator.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiResolveRequest.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiResolveResult.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiActionabilityResult.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiWaitRequest.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiWaitResult.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiInspectResult.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/api/runtime/`

**Step 1: Write the failing tests**

Add focused tests for:

- locator/request/result records round-tripping expected fields
- stable defaults and concise error-code behavior

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiResolve*" --tests "*UiInspectResult*" --no-daemon`
Expected: compile/test failure because the new models do not exist yet.

**Step 3: Write minimal implementation**

Add the new low-level request/result records with only the fields required by the design.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiResolve*" --tests "*UiInspectResult*" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/api/runtime Mod/src/test/java/dev/vfyjxf/mcp/api/runtime
git commit -m "feat: add ui driver request and result models"
```

### Task 2: Add default driver support for resolve, actionability, inspect, and wait

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiDriver.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/DefaultUiDriverSupport.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/DefaultUiDriverSupportTest.java`

**Step 1: Write the failing tests**

Cover:

- default resolve using snapshot/query
- ambiguous target reporting
- actionability rejecting hidden, disabled, and unsupported actions
- default inspect composing summary and interaction state
- default wait polling until a condition matches or times out

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*DefaultUiDriverSupportTest" --no-daemon`
Expected: FAIL because the new support layer and default methods do not exist.

**Step 3: Write minimal implementation**

Add the support helper and wire `UiDriver` default methods to use it.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*DefaultUiDriverSupportTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiDriver.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/DefaultUiDriverSupport.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/DefaultUiDriverSupportTest.java
git commit -m "feat: add default ui driver support semantics"
```

### Task 3: Upgrade vanilla driver overrides

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaScreenUiDriver.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaWidgetPressSupport.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/VanillaScreenUiDriverTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/VanillaWidgetPressSupportTest.java`

**Step 1: Write the failing tests**

Add tests for:

- locator resolution by text / containsText / id / index / scope
- stronger click/hover actionability
- inspect summary shape for vanilla widget screens

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*VanillaScreenUiDriverTest" --tests "*VanillaWidgetPressSupportTest" --no-daemon`
Expected: FAIL on the new expectations.

**Step 3: Write minimal implementation**

Override the default semantics where vanilla widgets can provide better behavior.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*VanillaScreenUiDriverTest" --tests "*VanillaWidgetPressSupportTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaScreenUiDriver.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaWidgetPressSupport.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/VanillaScreenUiDriverTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/VanillaWidgetPressSupportTest.java
git commit -m "feat: strengthen vanilla ui driver semantics"
```

### Task 4: Add high-level inspect and act tools on top of the driver API

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing tests**

Add tests for:

- `moddev.ui_inspect` concise default payload
- `moddev.ui_act` using `ref`
- `moddev.ui_act` using locator
- actionability failure mapping with stable error codes

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because the new tools are not registered yet.

**Step 3: Write minimal implementation**

Register the new high-level tools and map them to the enhanced driver API.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "feat: add high-level ui inspect and act tools"
```

### Task 5: Add unified wait tool and trace exposure

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManager.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationBatchTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationTraceTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationErrorHandlingTest.java`

**Step 1: Write the failing tests**

Add tests for:

- `moddev.ui_wait` unified condition handling
- trace entries for high-level inspect/act/wait flows
- timeout and stale resolution using stable error codes

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiAutomationTraceTest" --tests "*UiAutomationErrorHandlingTest" --tests "*UiAutomationBatchTest" --no-daemon`
Expected: FAIL because the new wait/trace behavior is not implemented yet.

**Step 3: Write minimal implementation**

Add the high-level wait tool and record concise trace entries for the new tool family.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiAutomationTraceTest" --tests "*UiAutomationErrorHandlingTest" --tests "*UiAutomationBatchTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManager.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationBatchTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationTraceTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationErrorHandlingTest.java
git commit -m "feat: add unified ui wait and trace tools"
```

### Task 6: Update docs and verify the end state

**Files:**
- Modify: `README.md`
- Modify: `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- Modify: `docs/plans/2026-03-12-playwright-style-ui-automation/impl.md`

**Step 1: Write the failing documentation checklist**

List the new high-level tools, low-level driver enhancements, and recommended agent workflow.

**Step 2: Run verification**

Run: `.\gradlew.bat :Mod:test :Server:test --no-daemon`
Expected: PASS.

**Step 3: Update documentation**

Document:

- high-level workflow: inspect -> act -> wait -> screenshot
- low-level tool fallback path
- concise schema conventions
- stable error-code usage

**Step 4: Run verification again**

Run: `.\gradlew.bat :Mod:test :Server:test --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add README.md docs/guides/2026-03-12-playwright-style-ui-automation-guide.md docs/plans/2026-03-12-playwright-style-ui-automation/impl.md
git commit -m "docs: update playwright-style ui debugging workflow"
```
