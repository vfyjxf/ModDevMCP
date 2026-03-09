# UI Capture Artifact Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** add a runnable placeholder PNG capture pipeline with local file output and server-side resource reads.

**Architecture:** `Mod` renders and stores PNG artifacts while `Server` exposes a minimal read-only resource registry. `ui_capture` returns both debug-local and formal resource references so the later real renderer can slot in without changing the tool contract.

**Tech Stack:** Java 21, Gradle, JUnit 5, AWT `BufferedImage` and `ImageIO`

---

### Task 1: Add failing ui_capture artifact tests

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing test**

Add assertions that `ui_capture` returns:
- `imageRef`
- `imagePath`
- `imageResourceUri`
- `imageMeta.format == "png"`

Also assert the returned file exists.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because no image artifact fields exist yet.

**Step 3: Write minimal implementation**

Defer until Task 2 and Task 3.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "test: cover ui capture image artifacts"
```

### Task 2: Add artifact store and renderer

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureArtifact.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureArtifactStore.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureRenderer.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureArtifactStoreTest.java`

**Step 1: Write the failing test**

Test that storing a rendered PNG:
- returns a unique `imageRef`
- creates a real file
- preserves `image/png` metadata

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiCaptureArtifactStoreTest" --no-daemon`
Expected: FAIL because store and renderer do not exist.

**Step 3: Write minimal implementation**

Implement a simple PNG renderer and disk-backed artifact store under a deterministic local directory.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiCaptureArtifactStoreTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureArtifact.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureArtifactStore.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureRenderer.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureArtifactStoreTest.java
git commit -m "feat: add ui capture artifact store"
```

### Task 3: Add minimal server resource registry

**Files:**
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/api/McpResource.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/api/McpResourceProvider.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/runtime/McpResourceRegistry.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/ModDevMcpServer.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/runtime/McpResourceRegistryTest.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/ModDevMcpServerTest.java`

**Step 1: Write the failing test**

Add tests asserting:
- providers can register and resolve URIs
- `ModDevMcpServer` exposes the resource registry

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Server:test --tests "*McpResourceRegistryTest" --tests "*ModDevMcpServerTest" --no-daemon`
Expected: FAIL because resource registry APIs do not exist.

**Step 3: Write minimal implementation**

Implement read-only resource registration and lookup by URI.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Server:test --tests "*McpResourceRegistryTest" --tests "*ModDevMcpServerTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Server/src/main/java/dev/vfyjxf/mcp/server/api/McpResource.java Server/src/main/java/dev/vfyjxf/mcp/server/api/McpResourceProvider.java Server/src/main/java/dev/vfyjxf/mcp/server/runtime/McpResourceRegistry.java Server/src/main/java/dev/vfyjxf/mcp/server/ModDevMcpServer.java Server/src/test/java/dev/vfyjxf/mcp/server/runtime/McpResourceRegistryTest.java Server/src/test/java/dev/vfyjxf/mcp/server/ModDevMcpServerTest.java
git commit -m "feat: add mcp resource registry"
```

### Task 4: Wire capture artifacts into ui_capture

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Create or Modify: capture resource provider class in `Mod`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing test**

Add assertions that:
- `imageResourceUri` starts with `moddev://capture/`
- the server resource registry can resolve the returned URI

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because `ui_capture` is not yet wired to the artifact store and server resources.

**Step 3: Write minimal implementation**

Connect `UiToolProvider` to the renderer and artifact store, and register a capture resource provider during mod bootstrap.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java ...
git commit -m "feat: expose ui capture image artifacts"
```

### Task 5: Full verification

**Files:**
- Modify: none unless regressions are found

**Step 1: Run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --tests "*UiCaptureArtifactStoreTest" --no-daemon`
Expected: PASS

**Step 2: Run server tests**

Run: `.\gradlew.bat :Server:test --tests "*McpResourceRegistryTest" --tests "*ModDevMcpServerTest" --no-daemon`
Expected: PASS

**Step 3: Run full verification**

Run: `.\gradlew.bat :Mod:test :Server:test --no-daemon`
Expected: PASS

**Step 4: Inspect git status**

Run: `git status --short`
Expected: only intended files changed or clean after commit
