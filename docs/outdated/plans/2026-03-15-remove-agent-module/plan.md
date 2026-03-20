# Remove Agent Module Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove the standalone `Agent` module, stop injecting `-javaagent`, and resolve hotswap instrumentation directly from `net.lenni0451:Reflect`.

**Architecture:** The `Mod` project becomes the only place that knows about instrumentation lookup. The Gradle plugin keeps runtime host wiring but no longer resolves or injects a separate agent artifact. User-facing DSL and docs are updated to match the simpler model.

**Tech Stack:** Gradle plugin API, NeoForge ModDevGradle, JUnit 5, Reflect 1.6.2

---

### Task 1: Write failing tests for the no-agent plugin model

**Files:**
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/ModDevMcpPluginTest.java`
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/NeoForgeRunInjectorTest.java`

**Step 1: Write the failing test**

- Update extension-surface assertions to remove `agentVersion` and `agentJarPath`.
- Update run injector tests to assert runtime properties still exist but no `-javaagent` is injected.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
Expected: FAIL because the plugin still exposes agent fields and still injects `-javaagent`.

**Step 3: Write minimal implementation**

- Remove agent fields from the extension and all related plugin logic.
- Keep only non-agent runtime wiring.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
Expected: PASS

### Task 2: Write failing tests for hotswap instrumentation lookup without `HotswapAgent`

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/hotswap/HotswapServiceIntegrationTest.java`

**Step 1: Write the failing test**

- Change the “agent missing” assertion so it expects a generic instrumentation-unavailable message instead of a `HotswapAgent`-specific message.
- Keep diagnostics coverage but stop asserting keys tied to agent classloader/version lookup.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*HotswapServiceIntegrationTest" --no-daemon`
Expected: FAIL because `HotswapService` still uses `HotswapAgent`-specific lookup and messages.

**Step 3: Write minimal implementation**

- Replace the agent-class reflection path with direct `Reflect Agents` instrumentation lookup.
- Update diagnostics and error messages to the new source.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*HotswapServiceIntegrationTest" --no-daemon`
Expected: PASS

### Task 3: Remove the standalone Agent module from the build

**Files:**
- Modify: `settings.gradle`
- Modify: `gradle.properties`
- Delete: `Agent/build.gradle`
- Delete: `Agent/src/main/java/dev/vfyjxf/mcp/agent/HotswapAgent.java`
- Delete: `Agent/src/main/java/dev/vfyjxf/mcp/agent/HotswapCapabilities.java`
- Modify: `Mod/build.gradle`

**Step 1: Write the failing test**

- Use the tests from Task 1 and Task 2 as the forcing function.

**Step 2: Run test to verify it fails**

Run: `rg -n ":Agent|moddevmcp-agent|agent_version|HotswapAgent" settings.gradle gradle.properties Mod Plugin Agent -g "*.gradle" -g "*.java"`
Expected: matches found before cleanup.

**Step 3: Write minimal implementation**

- Remove `:Agent` from the composite build.
- Remove agent version property and agent project references.
- Add `Reflect` dependency to `Mod`.
- Remove run/test `-javaagent` configuration from `Mod`.

**Step 4: Run verification**

Run: `rg -n ":Agent|moddevmcp-agent|agent_version|HotswapAgent" settings.gradle gradle.properties Mod Plugin Agent -g "*.gradle" -g "*.java"`
Expected: no production-code matches after cleanup

### Task 4: Update docs and record real verification

**Files:**
- Modify: `README.md`
- Modify: `README.zh.md`
- Modify: relevant files under `docs/guides/`
- Create/Modify: `docs/plans/2026-03-15-remove-agent-module/checklist.md`
- Create/Modify: `docs/plans/2026-03-15-remove-agent-module/impl.md`

**Step 1: Write the failing test**

- Use text search to find outdated agent references in user-facing docs.

**Step 2: Run test to verify it fails**

Run: `rg -n "agentVersion|agentJarPath|moddevmcp-agent|-javaagent|HotswapAgent" README.md README.zh.md docs/guides Plugin Mod TestMod -g "*.md" -g "*.gradle" -g "*.java"`
Expected: matches found before cleanup.

**Step 3: Write minimal implementation**

- Remove agent-related user configuration and old architecture wording.
- Record exact commands and outcomes in plan docs.

**Step 4: Run test to verify it passes**

Run: `rg -n "agentVersion|agentJarPath|moddevmcp-agent|-javaagent|HotswapAgent" README.md README.zh.md docs/guides Plugin Mod TestMod -g "*.md" -g "*.gradle" -g "*.java"`
Expected: only intended historical plan mentions remain

### Task 5: Run end-to-end verification

**Files:**
- Verify generated output under `TestMod/build/moddevmcp/mcp-clients`

**Step 1: Run focused verification**

Run: `.\gradlew.bat -p Plugin test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
Expected: PASS

**Step 2: Run mod verification**

Run: `.\gradlew.bat :Mod:test --tests "*HotswapServiceIntegrationTest" --no-daemon`
Expected: PASS

**Step 3: Run consumer verification**

Run: `.\TestMod\gradlew.bat -p . createMcpClientFiles --no-daemon`
Expected: PASS
