# Plugin Provider Defaults Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Minimize the public `modDevMcp` DSL and move plugin defaults/wiring onto Gradle providers and task/source set APIs.

**Architecture:** Keep only the public knobs that users actually need, and compute the rest inside the plugin from the Java/NeoForge project model. `createMcpClientFiles` and NeoForge run injection should read from provider-backed internal defaults instead of user-facing string fields. Remove `ModDevClientRunFlags` and let the plugin own the run wiring directly.

**Tech Stack:** Gradle plugin API, Gradle Provider API, NeoForge ModDevGradle, JUnit 5

---

### Task 1: Write failing tests for reduced public DSL and provider-backed defaults

**Files:**
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/ModDevMcpPluginTest.java`
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/NeoForgeRunInjectorTest.java`

**Step 1: Write the failing test**

- Add a plugin test that verifies the plugin supplies the MCP runtime classpath automatically from the published `moddevmcp-server` dependency.
- Add a run injector test that verifies the plugin derives compile task and class output without requiring user-provided `projectRoot`, `compileTask`, or `classOutputDir`.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Plugin:test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
Expected: FAIL because the plugin still depends on user-facing string fields or does not fully derive defaults from Gradle model/provider state.

**Step 3: Write minimal implementation**

- Keep only necessary public DSL fields.
- Derive runtime classpath, compile task, class output, and output directories internally.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Plugin:test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
Expected: PASS

### Task 2: Refactor extension and plugin internals to Provider-based defaults

**Files:**
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpExtension.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpPlugin.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/CreateMcpClientFilesTask.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/neoforge/NeoForgeRunInjector.java`

**Step 1: Write the failing test**

- Use the tests from Task 1 as the forcing function.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Plugin:test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
Expected: FAIL before the refactor is complete.

**Step 3: Write minimal implementation**

- Remove public extension fields for `projectRoot`, `compileTask`, `classOutputDir`, `mcpMainClass`, `backendMainClass`, `javaCommand`, `mcpHost`, `mcpPort`, `mcpProxyPort`, `mcpRuntimeClasspath`, and `mcpClientFilesOutputDir`.
- Register internal provider-backed defaults in the plugin:
  - project directory from `project.getLayout().getProjectDirectory()`
  - compile task from `tasks.named("compileJava")`
  - class output from `sourceSets.main.output.classesDirs`
  - java executable from current toolchain/java home provider
  - fixed host/ports/main classes as internal constants
  - runtime classpath from the plugin-owned `modDevMcpRuntime` configuration
  - output dir from `layout.buildDirectory.dir("moddevmcp/mcp-clients")`
- Pass those providers into `createMcpClientFiles`.
- Update run injection to consume internal providers instead of public string fields.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Plugin:test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
Expected: PASS

### Task 3: Remove `ModDevClientRunFlags` and let the plugin own run configuration

**Files:**
- Modify: `Mod/build.gradle`
- Modify: `buildSrc/.../ModDevClientRunFlags.*` or delete the file if unused
- Search: `rg -n "ModDevClientRunFlags" .`

**Step 1: Write the failing test**

- No new test file required if plugin/run tests already cover the desired defaults.

**Step 2: Run test to verify it fails**

Run: `rg -n "ModDevClientRunFlags" .`
Expected: existing references found.

**Step 3: Write minimal implementation**

- Remove the `ModDevClientRunFlags` import/usage from the mod build.
- Keep run configuration behavior inside the plugin-run wiring instead of buildSrc helpers.

**Step 4: Run verification**

Run: `rg -n "ModDevClientRunFlags" .`
Expected: no remaining references.

### Task 4: Simplify TestMod and docs to the minimal DSL

**Files:**
- Modify: `TestMod/build.gradle`
- Modify: `README.md`
- Modify: `README.zh.md`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.md`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.zh.md`
- Create/Modify: `docs/plans/2026-03-14-plugin-provider-defaults/checklist.md`
- Create/Modify: `docs/plans/2026-03-14-plugin-provider-defaults/impl.md`

**Step 1: Write the failing test**

- Use text search as the safety net: old DSL fields should disappear from user docs and `TestMod`.

**Step 2: Run test to verify it fails**

Run: `rg -n "projectRoot|compileTask|classOutputDir|mcpRuntimeClasspath|mcpClientFilesOutputDir|agentVersion = \"\\$\\{agent_version\\}\"" TestMod README.md README.zh.md docs/guides -g "*.gradle" -g "*.md"`
Expected: matches found before cleanup.

**Step 3: Write minimal implementation**

- Reduce `TestMod` to the minimal public DSL.
- Update docs to show the new minimal setup and note that the plugin now owns the defaults.
- Record real verification results in checklist/impl docs.

**Step 4: Run test to verify it passes**

Run: `rg -n "projectRoot|compileTask|classOutputDir|mcpRuntimeClasspath|mcpClientFilesOutputDir|agentVersion = \"\\$\\{agent_version\\}\"" TestMod README.md README.zh.md docs/guides -g "*.gradle" -g "*.md"`
Expected: no matches in user-facing docs/TestMod except where historical plan docs intentionally mention them.

### Task 5: Run end-to-end verification

**Files:**
- Verify generated output under `TestMod/build/moddevmcp/mcp-clients`

**Step 1: Run focused verification**

Run: `.\TestMod\gradlew.bat -p . createMcpClientFiles --no-daemon`
Expected: PASS and generated client files exist.

**Step 2: Inspect generated classpath**

Run: `Get-Content TestMod\build\moddevmcp\mcp-clients\classpath.txt`
Expected: contains the resolved `moddevmcp-server` artifact or composite-build server jar plus runtime dependencies.

**Step 3: Record results**

- Update `docs/plans/2026-03-14-plugin-provider-defaults/checklist.md`
- Update `docs/plans/2026-03-14-plugin-provider-defaults/impl.md`

