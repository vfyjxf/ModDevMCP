# Hotswap Consumer Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a standard downstream hotswap integration path using a NeoForge-focused Gradle plugin, while preserving manual JVM argument configuration as a fallback.

**Architecture:** Introduce a new Gradle plugin module that resolves and injects the hotswap agent plus runtime system properties into NeoForge runs. Update runtime hotswap code to consume plugin-provided configuration instead of assuming this repository's task names and class output layout. Publish mod, agent, and plugin as separate artifacts and document both plugin and manual setup.

**Tech Stack:** Gradle, Java 21, NeoForge ModDev plugin, Java Instrumentation API, JUnit

---

### Task 1: Create Plugin Module Skeleton

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle`
- Create: `Plugin/build.gradle`
- Create: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpHotswapPlugin.java`
- Create: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpHotswapExtension.java`
- Create: `Plugin/src/main/resources/META-INF/gradle-plugins/dev.vfyjxf.moddev-hotswap.properties`

**Step 1: Write the failing test**

Create a plugin functional/unit test scaffold that asserts the plugin id can be applied and creates the extension.

**Step 2: Run test to verify it fails**

Run: `gradlew.bat -g .gradle-user-home :Plugin:test --tests *ModDevMcpHotswapPluginTest`
Expected: FAIL because plugin module does not exist yet.

**Step 3: Write minimal implementation**

- Add `:Plugin` to `settings.gradle`
- Configure Java Gradle plugin support in `Plugin/build.gradle`
- Register plugin id `dev.vfyjxf.moddev-hotswap`
- Create the extension with properties:
  - `enabled`
  - `projectRoot`
  - `compileTask`
  - `classOutputDir`
  - `runs`
  - `requireEnhancedHotswap`

**Step 4: Run test to verify it passes**

Run: `gradlew.bat -g .gradle-user-home :Plugin:test --tests *ModDevMcpHotswapPluginTest`
Expected: PASS

**Step 5: Commit**

```bash
git add settings.gradle build.gradle Plugin
git commit -m "feat: add hotswap Gradle plugin module"
```

### Task 2: Add NeoForge Run Injection

**Files:**
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpHotswapPlugin.java`
- Create: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/neoforge/NeoForgeRunInjector.java`
- Test: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/NeoForgeRunInjectorTest.java`

**Step 1: Write the failing test**

Add tests that verify the plugin:

- requires `net.neoforged.moddev`
- injects `-javaagent`
- injects `moddevmcp.*` system properties into selected runs

**Step 2: Run test to verify it fails**

Run: `gradlew.bat -g .gradle-user-home :Plugin:test --tests *NeoForgeRunInjectorTest`
Expected: FAIL because injection logic is missing.

**Step 3: Write minimal implementation**

Implement a NeoForge-specific injector that:

- locates configured runs
- computes defaults
- appends:
  - `-javaagent:<resolved-agent-path>`
  - `-Dmoddevmcp.project.root=...`
  - `-Dmoddevmcp.compile.task=...`
  - `-Dmoddevmcp.class.output=...`

Keep the injector behind a small internal interface so additional platforms can be added later.

**Step 4: Run test to verify it passes**

Run: `gradlew.bat -g .gradle-user-home :Plugin:test --tests *NeoForgeRunInjectorTest`
Expected: PASS

**Step 5: Commit**

```bash
git add Plugin
git commit -m "feat: inject hotswap settings into NeoForge runs"
```

### Task 3: Resolve and Publish Agent Artifact Cleanly

**Files:**
- Modify: `Agent/build.gradle`
- Modify: `build.gradle`
- Modify: `Mod/build.gradle`
- Modify: `README.md`
- Test: `Agent` jar manifest verification

**Step 1: Write the failing test**

Add or define verification that published/consumable agent metadata exists and the plugin can resolve the agent coordinate expected for the current version.

**Step 2: Run test to verify it fails**

Run: `gradlew.bat -g .gradle-user-home :Agent:jar :Plugin:test --tests *AgentResolution*`
Expected: FAIL because publication metadata or resolution wiring is incomplete.

**Step 3: Write minimal implementation**

- Give `Agent` its own publishable coordinates
- Ensure manifest still contains:
  - `Premain-Class`
  - `Can-Redefine-Classes`
  - `Can-Retransform-Classes`
- Teach the plugin how to resolve the agent for its own version
- Remove assumptions that the consumer has a local `:Agent` project

**Step 4: Run test to verify it passes**

Run: `gradlew.bat -g .gradle-user-home :Agent:jar :Plugin:test --tests *AgentResolution*`
Expected: PASS

**Step 5: Commit**

```bash
git add Agent build.gradle Mod/build.gradle README.md Plugin
git commit -m "feat: publish and resolve hotswap agent artifact"
```

### Task 4: Make Runtime Hotswap Configuration Consumer-Driven

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/hotswap/HotswapService.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/hotswap/HotswapRuntimeConfig.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/hotswap/HotswapRuntimeConfigTest.java`

**Step 1: Write the failing test**

Add tests that verify runtime configuration can be read from system properties and overrides defaults for:

- project root
- compile task
- class output directory

**Step 2: Run test to verify it fails**

Run: `gradlew.bat -g .gradle-user-home :Mod:test --tests *HotswapRuntimeConfigTest`
Expected: FAIL because runtime config abstraction does not exist.

**Step 3: Write minimal implementation**

- Create `HotswapRuntimeConfig`
- Read:
  - `moddevmcp.project.root`
  - `moddevmcp.compile.task`
  - `moddevmcp.class.output`
- Update `HotswapService.compile()` to run the configured task
- Update class scanning to use the configured output directory
- Preserve compatibility defaults where reasonable

**Step 4: Run test to verify it passes**

Run: `gradlew.bat -g .gradle-user-home :Mod:test --tests *HotswapRuntimeConfigTest`
Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp Mod/src/test/java/dev/vfyjxf/mcp/runtime/hotswap
git commit -m "feat: make hotswap runtime configurable for consumer projects"
```

### Task 5: Improve Tool Surface and Error Reporting

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/HotswapToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/hotswap/HotswapService.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/HotswapToolProviderTest.java`

**Step 1: Write the failing test**

Add tests for:

- exposed `compile` argument in tool schema
- configuration-related errors
- capability reporting

**Step 2: Run test to verify it fails**

Run: `gradlew.bat -g .gradle-user-home :Mod:test --tests *HotswapToolProviderTest`
Expected: FAIL because schema and messages do not cover the new behavior.

**Step 3: Write minimal implementation**

- Document the optional `compile` argument in tool definition
- Return clearer errors when:
  - agent missing
  - compile task missing
  - class output directory missing
- Keep capability map in responses

**Step 4: Run test to verify it passes**

Run: `gradlew.bat -g .gradle-user-home :Mod:test --tests *HotswapToolProviderTest`
Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool
git commit -m "feat: improve hotswap tool schema and diagnostics"
```

### Task 6: Document Downstream Plugin and Manual Setup

**Files:**
- Modify: `README.md`
- Create: `docs/hotswap-consumer-setup.md`
- Modify: `docs/impl/2026-03-10-hotswap-implementation-details.md`

**Step 1: Write the failing documentation check**

Create a checklist that confirms docs cover:

- plugin-based setup
- manual fallback setup
- Java/JBR expectations
- supported scope and current limitations

**Step 2: Verify it fails**

Review docs and confirm these downstream setup sections are missing or outdated.

**Step 3: Write minimal documentation**

- Add plugin usage examples
- Add manual JVM argument examples
- Clarify that enhanced structural hotswap depends on JBR/DCEVM
- Fix implementation doc drift around runtime assumptions

**Step 4: Verify documentation is complete**

Review the checklist and confirm all items are covered.

**Step 5: Commit**

```bash
git add README.md docs
git commit -m "docs: add downstream hotswap integration guide"
```

### Task 7: End-to-End Verification

**Files:**
- Verify across `Agent`, `Plugin`, and `Mod`

**Step 1: Run plugin tests**

Run: `gradlew.bat -g .gradle-user-home :Plugin:test`
Expected: PASS

**Step 2: Run mod hotswap tests**

Run: `gradlew.bat -g .gradle-user-home :Mod:test --tests "*Hotswap*" --tests "*BuiltinProviderRegistrationTest"`
Expected: PASS

**Step 3: Run agent packaging verification**

Run: `gradlew.bat -g .gradle-user-home :Agent:jar`
Expected: PASS and jar manifest contains required agent attributes

**Step 4: Smoke-test a consumer sample or fixture**

Run a fixture build that applies the plugin and confirm the generated run config includes:

- `-javaagent`
- `moddevmcp.project.root`
- `moddevmcp.compile.task`
- `moddevmcp.class.output`

**Step 5: Commit final verification changes if needed**

```bash
git add .
git commit -m "test: verify downstream hotswap integration"
```
