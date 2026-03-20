# Host Rename And Codex Script Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rename the active `relay` terminology to `host`, move Codex launch-script generation support from `buildSrc` into `Plugin`, and verify the workflow from `TestMod`.

**Architecture:** Keep the current split unchanged: `Server` remains the MCP host module and `Mod` remains the game runtime client. This pass is a naming and tooling refactor only: rename active runtime-host terms, expose launch-file helpers from `Plugin`, and let `TestMod` generate Codex-facing launch artifacts for the current `Server` stdio entrypoint.

**Tech Stack:** Gradle composite build, Java 21, Groovy build scripts, JUnit 5

---

### Task 1: Add failing tests for the new active terminology

**Files:**
- Modify: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpStdioMainTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/RelayArchitectureDocsTest.java`

**Step 1: Write the failing test**

Add assertions that the active README/docs use `host` wording instead of `relay`.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Server:test --tests "*ModDevMcpStdioMainTest" --no-daemon`
Expected: FAIL while the current sources still contain `relay` wording

### Task 2: Add failing tests for launch-file helpers under `Plugin`

**Files:**
- Create: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/McpLaunchFilesTest.java`
- Delete: `buildSrc/src/test/java/dev/vfyjxf/gradle/EmbeddedMcpLaunchFilesTest.java`

**Step 1: Write the failing test**

Cover:
- quoted Java args output
- `.bat` launcher output
- TOML snippet output

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Plugin:test --tests "*McpLaunchFilesTest" --no-daemon`
Expected: FAIL because the helper has not been moved yet

### Task 3: Add failing TestMod checks for the Codex launch task

**Files:**
- Modify: `TestMod/build.gradle`

**Step 1: Write the failing test**

Add a TestMod task that should generate:
- `build/moddevmcp/codex/classpath.txt`
- `build/moddevmcp/codex/codex-mcp-java.args`
- `build/moddevmcp/codex/run-codex-mcp.bat`
- `build/moddevmcp/codex/moddevmcp-codex.toml`

**Step 2: Run task to verify it fails**

Run: `cd TestMod; .\gradlew.bat createCodexMcpLaunchScript --no-daemon`
Expected: FAIL before the helper and task wiring exist

### Task 4: Implement the rename and script generation

**Files:**
- Modify: active `Server/src/main/java/dev/vfyjxf/mcp/server/**`
- Modify: active `Mod/src/main/java/dev/vfyjxf/mcp/runtime/**`
- Create: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/McpLaunchFiles.java`
- Modify: `Plugin/build.gradle`
- Modify: `TestMod/build.gradle`
- Modify: active docs under `README.md`, `docs/guides/`, `docs/plans/2026-03-13-relay-server-runtime-split/`
- Delete: `buildSrc/src/main/java/dev/vfyjxf/gradle/EmbeddedMcpLaunchFiles.java`
- Delete: `buildSrc/src/test/java/dev/vfyjxf/gradle/EmbeddedMcpLaunchFilesTest.java`

**Step 1: Write minimal implementation**

- rename active runtime-host classes and messages from `relay` to `host`
- move launch-file helper into `Plugin`
- wire `TestMod` task to generate Codex launch artifacts for `dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain`

**Step 2: Run focused verification**

Run: `.\gradlew.bat :Server:test --tests "*ModDevMcpStdioMainTest" --no-daemon`
Expected: PASS

Run: `.\gradlew.bat :Plugin:test --tests "*McpLaunchFilesTest" --no-daemon`
Expected: PASS

Run: `cd TestMod; .\gradlew.bat createCodexMcpLaunchScript --no-daemon`
Expected: PASS and artifacts generated
