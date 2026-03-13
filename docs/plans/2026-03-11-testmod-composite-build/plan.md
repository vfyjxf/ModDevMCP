# TestMod Composite Build Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a standalone `TestMod/` composite-build project and shift real Minecraft verification to `TestMod`'s Gradle `runClient`.

**Architecture:** `TestMod/` is a separate Gradle project at repository root that uses `includeBuild("..")` to depend on the MCP repository. It owns the real integration `runClient`, while shared stable-server config still comes from the main repository's `Server` and `Mod` implementation.

**Tech Stack:** Gradle 9, NeoForge ModDevGradle, composite builds, Java 21, existing stable-server lifecycle/config wiring.

---

### Task 1: Add design assertions for the new workflow

**Files:**
- Modify: `README.md`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/EmbeddedModDevMcpStdioMainTest.java`

**Step 1: Write the failing test**

Add assertions that the README documents `TestMod` as the preferred real-game `runClient` entrypoint.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*EmbeddedModDevMcpStdioMainTest" --no-daemon`

Expected: FAIL because `README.md` does not yet mention the new `TestMod` workflow.

**Step 3: Write minimal documentation update**

Update `README.md` to describe:

- `TestMod/` exists
- `TestMod` is started with its own Gradle `runClient`
- this is the preferred real-game integration path

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*EmbeddedModDevMcpStdioMainTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add README.md Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/EmbeddedModDevMcpStdioMainTest.java
git commit -m "test: document testmod runClient workflow"
```

### Task 2: Scaffold the standalone `TestMod` project

**Files:**
- Create: `TestMod/settings.gradle`
- Create: `TestMod/build.gradle`
- Create: `TestMod/gradle.properties`
- Create: `TestMod/src/main/java/dev/vfyjxf/testmod/TestModEntrypoint.java`
- Create: `TestMod/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `TestMod/src/main/templates/META-INF/neoforge.mods.toml`

**Step 1: Write the failing test**

Add a test in the main repo that asserts the `TestMod` composite project files exist and declare `includeBuild("..")`.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*TestModProjectLayoutTest" --no-daemon`

Expected: FAIL because `TestMod/` does not exist yet.

**Step 3: Write minimal implementation**

Create the `TestMod/` Gradle project with:

- independent `settings.gradle`
- composite build include of `..`
- NeoForge mod setup
- basic no-op mod entrypoint

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*TestModProjectLayoutTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add TestMod
git commit -m "feat: add standalone testmod composite project"
```

### Task 3: Wire composite dependencies to the MCP implementation

**Files:**
- Modify: `TestMod/settings.gradle`
- Modify: `TestMod/build.gradle`
- Add/Modify tests under: `Mod/src/test/java/dev/vfyjxf/mcp/...`

**Step 1: Write the failing test**

Add a focused test that checks the generated `TestMod/build.gradle` text documents the intended included-build dependency and stable-server config wiring.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*TestModBuildConfigTest" --no-daemon`

Expected: FAIL because dependency/config wiring is incomplete.

**Step 3: Write minimal implementation**

Configure `TestMod` so its dev/runtime setup:

- consumes MCP mod output from the included build
- can see the needed runtime classes for the MCP mod side
- passes shared stable-server config properties into `runClient`

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*TestModBuildConfigTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add TestMod Mod/src/test/java/dev/vfyjxf/mcp
git commit -m "feat: wire testmod composite build to mcp runtime"
```

### Task 4: Make `TestMod:runClient` the preferred real-game startup path

**Files:**
- Modify: `README.md`
- Create: `docs/guides/2026-03-11-testmod-runclient-guide.md`
- Modify: any existing fixed-port guide that still points primary real-game startup at `:Mod:runClient`

**Step 1: Write the failing test**

Add or extend README/guide assertions so the preferred real-game path points to `TestMod`.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*EmbeddedModDevMcpStdioMainTest" --no-daemon`

Expected: FAIL on outdated docs text.

**Step 3: Write minimal implementation**

Document:

- how to enter `TestMod/`
- how to run its `runClient`
- how it relates to the stable server
- why scripts are now secondary debug artifacts

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*EmbeddedModDevMcpStdioMainTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add README.md docs/guides/2026-03-11-testmod-runclient-guide.md
git commit -m "docs: make testmod runClient the primary game workflow"
```

### Task 5: Verify the composite build config can resolve and run Gradle tasks

**Files:**
- Verify only

**Step 1: Run Gradle task listing in `TestMod`**

Run: `cd TestMod; .\gradlew.bat tasks --all`

Expected: `runClient` is available.

**Step 2: Run compile verification**

Run: `cd TestMod; .\gradlew.bat compileJava --no-daemon`

Expected: PASS

**Step 3: Run real client startup**

Run: `cd TestMod; .\gradlew.bat runClient --no-daemon`

Expected: client starts, stable server config is honored, and runtime lifecycle becomes connected.

**Step 4: Verify MCP integration**

Run the existing local MCP probe against the stable server after `TestMod:runClient` starts.

Expected:

- stable server port listens
- lifecycle file updates
- MCP runtime calls respond from the live game path

**Step 5: Commit**

```bash
git add README.md docs/guides docs/plans
git commit -m "test: verify testmod composite runClient workflow"
```

### Task 6: Write execution docs for future testing

**Files:**
- Create: `docs/plans/2026-03-11-testmod-composite-build/checklist.md`
- Create: `docs/plans/2026-03-11-testmod-composite-build/impl.md`

**Step 1: Write checklist**

Record implementation items and verification checkpoints with timestamps.

**Step 2: Write impl summary**

Record the exact commands, outputs, failures, and whether any failure was code-side or environment-side.

**Step 3: Verify docs exist**

Run: `Get-ChildItem docs/plans/2026-03-11-testmod-composite-build-*`

Expected: checklist and impl docs both exist.

**Step 4: Commit**

```bash
git add docs/plans
git commit -m "docs: record testmod composite build implementation"
```
