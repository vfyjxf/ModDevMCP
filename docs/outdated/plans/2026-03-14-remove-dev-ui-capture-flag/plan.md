# Remove Dev UI Capture Flag Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove the unused `moddevmcp.devUiCapture` property chain from runtime code, build scripts, tests, and docs.

**Architecture:** This is a deletion-focused cleanup. The implementation removes the runtime-side auto-verifier hook, removes the build-side system property forwarding helper, and updates tests and docs to reflect that no dev capture flag exists anymore.

**Tech Stack:** Java 21, Gradle, NeoForge ModDev plugin, JUnit 5, Markdown docs

---

### Task 1: Document the cleanup scope

**Files:**
- Create: `docs/plans/2026-03-14-remove-dev-ui-capture-flag/design.md`
- Create: `docs/plans/2026-03-14-remove-dev-ui-capture-flag/plan.md`
- Create: `docs/plans/2026-03-14-remove-dev-ui-capture-flag/checklist.md`

**Step 1: Write the design and plan docs**

Describe the removal scope, the affected code paths, and what must stay untouched.

**Step 2: Verify the docs exist**

Run: `rg -n "Remove Dev UI Capture Flag|moddevmcp.devUiCapture" docs/plans/2026-03-14-remove-dev-ui-capture-flag`
Expected: matches in the new plan folder

### Task 2: Remove test expectations for the flag

**Files:**
- Modify: `buildSrc/src/test/java/dev/vfyjxf/gradle/ModDevClientRunFlagsTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/ClientDevUiCaptureVerifierTest.java`

**Step 1: Write the failing test changes**

- Replace the `buildSrc` test with assertions that only MCP host/port are forwarded
- Remove or replace the `Mod` verifier-flag tests so they no longer reference `DevUiCaptureFlags`

**Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :buildSrc:test --tests "*ModDevClientRunFlagsTest" :Mod:test --tests "*ClientDevUiCaptureVerifierTest" --no-daemon`
Expected: FAIL because production code still references the removed flag chain

### Task 3: Delete the runtime/build flag implementation

**Files:**
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/DevUiCaptureFlags.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/ClientDevUiCaptureVerifier.java`
- Delete: `buildSrc/src/main/java/dev/vfyjxf/gradle/ModDevClientRunFlags.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/build.gradle`
- Modify: `TestMod/build.gradle`

**Step 1: Delete the removed classes**

Delete the two runtime classes and the build helper.

**Step 2: Remove all call sites**

- Remove `ClientDevUiCaptureVerifier` import and attach call from `ClientEntrypoint`
- Inline or simplify `Mod/build.gradle` run property handling so only required MCP properties remain
- Remove the dev UI capture system property forwarding from `TestMod/build.gradle`

**Step 3: Run focused tests**

Run: `.\gradlew.bat :buildSrc:test --tests "*ModDevClientRunFlagsTest" :Mod:test --tests "*ClientDevUiCaptureVerifierTest" --no-daemon`
Expected: PASS

### Task 4: Clean docs and verify repository state

**Files:**
- Modify: `README.md`
- Modify: `docs/guides/2026-03-11-codex-screenshot-demo-guide.md`
- Modify: `docs/plans/2026-03-11-capture-failure-on-fallback/impl.md`
- Modify: `docs/plans/2026-03-10-server-protocol-bootstrap/plan.md`
- Modify any other file still matching `moddevmcp.devUiCapture`

**Step 1: Remove stale documentation references**

Delete examples or wording that mention the removed property.

**Step 2: Verify no stale references remain**

Run: `rg -n "moddevmcp\\.devUiCapture|DEV_UI_CAPTURE_PROPERTY|ClientDevUiCaptureVerifier|DevUiCaptureFlags|ModDevClientRunFlags" .`
Expected: no matches except the historical cleanup plan folder if it intentionally documents the removal

**Step 3: Run full verification**

Run: `.\gradlew.bat test --no-daemon`
Expected: `BUILD SUCCESSFUL`
