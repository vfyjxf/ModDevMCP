# Plugin Agent Version Property Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace `agentCoordinates` with a simpler `agentVersion` property while fixing published dependency examples to use `dev.vfyjxf:moddevmcp`.

**Architecture:** Keep the plugin responsible for the fixed Maven coordinates and only expose the version as public configuration. Resolve the agent jar from `dev.vfyjxf:moddevmcp-agent:<agentVersion>` unless `agentJarPath` overrides it. Update tests, sample builds, and user docs to match the new public surface.

**Tech Stack:** Java 21, Gradle plugin development, JUnit 5, Markdown

---

### Task 1: Add plan docs for the plugin configuration change

**Files:**
- Create: `docs/plans/2026-03-14-plugin-agent-version-property/design.md`
- Create: `docs/plans/2026-03-14-plugin-agent-version-property/plan.md`
- Create: `docs/plans/2026-03-14-plugin-agent-version-property/checklist.md`
- Create: `docs/plans/2026-03-14-plugin-agent-version-property/impl.md`

**Step 1: Write the docs**

Document the new `agentVersion` model and the published dependency rename.

**Step 2: Verify the plan directory**

Run: `Get-ChildItem docs\plans\2026-03-14-plugin-agent-version-property`
Expected: the directory contains all four docs

### Task 2: Write failing plugin tests for `agentVersion`

**Files:**
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/NeoForgeRunInjectorTest.java`
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/ModDevMcpPluginTest.java`

**Step 1: Write failing tests**

Cover:
- the extension exposes `agentVersion`
- the default value is resolved from the plugin version inference path
- the run injector resolves `dev.vfyjxf:moddevmcp-agent:<agentVersion>`
- `agentJarPath` still overrides Maven resolution

**Step 2: Run tests to verify RED**

Run: `.\gradlew.bat :Plugin:test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
Expected: FAIL because `agentVersion` does not exist yet

### Task 3: Implement the new extension model

**Files:**
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpExtension.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/ModDevMcpPlugin.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/neoforge/NeoForgeRunInjector.java`

**Step 1: Replace extension property**

Remove `agentCoordinates` and add `agentVersion`.

**Step 2: Fix default version resolution**

Set `agentVersion` from the existing default version inference logic.

**Step 3: Resolve fixed coordinates internally**

Resolve `dev.vfyjxf:moddevmcp-agent:<agentVersion>` inside the plugin.

**Step 4: Run plugin tests to verify GREEN**

Run: `.\gradlew.bat :Plugin:test --tests "*ModDevMcpPluginTest" --tests "*NeoForgeRunInjectorTest" --no-daemon`
Expected: PASS

### Task 4: Update sample builds and user docs

**Files:**
- Modify: `TestMod/build.gradle`
- Modify: `README.md`
- Modify: `README.zh.md`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.md`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.zh.md`

**Step 1: Update public configuration examples**

Replace:
- `agentCoordinates = ...` with `agentVersion = ...`
- `dev.vfyjxf:moddevmcp-mod` with `dev.vfyjxf:moddevmcp`

**Step 2: Verify references**

Run: `rg -n "agentCoordinates|moddevmcp-mod" README.md README.zh.md docs TestMod Plugin -g "*.md" -g "*.gradle" -g "*.java"`
Expected: no remaining user-facing references except historical plan docs

### Task 5: Run integration verification and record real results

**Files:**
- Modify: `docs/plans/2026-03-14-plugin-agent-version-property/checklist.md`
- Modify: `docs/plans/2026-03-14-plugin-agent-version-property/impl.md`

**Step 1: Run a TestMod task**

Run: `.\TestMod\gradlew.bat -p .\TestMod createMcpClientFiles --no-daemon`
Expected: succeeds, or clearly fails only because of external dependency/network issues

**Step 2: Record actual results**

Write the real outputs and outcomes into `impl.md`
