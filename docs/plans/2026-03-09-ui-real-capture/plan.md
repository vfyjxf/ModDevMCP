# UI Real Capture Providers Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** add off-screen and framebuffer-based real capture providers to `ui_capture` while preserving placeholder fallback and the current artifact/resource contract.

**Architecture:** introduce two registries and two provider SPIs in `Mod`, have `ui_capture` select a real provider by requested source and priority, and keep the existing artifact store as the only file/resource publisher. Built-in providers implement vanilla off-screen and framebuffer capture, while placeholder rendering remains the final fallback.

**Tech Stack:** Java 21, NeoForge client classes, `NativeImage`, `TextureTarget`, JUnit 5

---

### Task 1: Add failing provider-selection tests

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing test**

Add tests asserting:
- `ui_capture` returns `imageMeta.source = "placeholder"` by default in the current test environment
- a custom offscreen provider is selected under `source=auto`
- a custom framebuffer provider is selected under `source=framebuffer`
- under `source=auto`, offscreen wins over framebuffer when both match

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because provider APIs and selection logic do not exist yet.

**Step 3: Write minimal implementation**

Defer to Task 2 and Task 3.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "test: cover ui real capture provider selection"
```

### Task 2: Add capture provider APIs and registries

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiCaptureImage.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiOffscreenCaptureProvider.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiFramebufferCaptureProvider.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/UiOffscreenCaptureProviderRegistry.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/UiFramebufferCaptureProviderRegistry.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/ModMcpApi.java`

**Step 1: Write the failing test**

Use the tests from Task 1.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because the registries and API types are missing.

**Step 3: Write minimal implementation**

Implement shared capture result model plus priority-based provider registries and registration methods on `ModMcpApi`.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: still FAIL until Task 3 wires `ui_capture`.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiCaptureImage.java Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiOffscreenCaptureProvider.java Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiFramebufferCaptureProvider.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/UiOffscreenCaptureProviderRegistry.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/UiFramebufferCaptureProviderRegistry.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java Mod/src/main/java/dev/vfyjxf/mcp/api/ModMcpApi.java
git commit -m "feat: add ui capture provider apis"
```

### Task 3: Wire `ui_capture` source selection and placeholder fallback

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureArtifactStore.java`

**Step 1: Write the failing test**

Use the tests from Task 1.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because `ui_capture` does not inspect `source` or provider registries.

**Step 3: Write minimal implementation**

Make `ui_capture`:
- parse `source`
- try offscreen or framebuffer provider registries as requested
- fall back to placeholder renderer
- tag `imageMeta.source` and `imageMeta.providerId`

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiCaptureArtifactStore.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "feat: add ui capture source selection"
```

### Task 4: Add built-in real providers

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/BuiltinUiCaptureProviders.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaOffscreenCaptureProvider.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaFramebufferCaptureProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`

**Step 1: Write the failing test**

Add a focused test asserting the built-in providers register into runtime and do not break the current placeholder path in tests.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because built-in provider registration does not exist.

**Step 3: Write minimal implementation**

Implement built-in providers with conservative matching:
- offscreen provider re-renders only when the live current screen matches the requested context
- framebuffer provider reads the current main render target only when a live screen is available
- both should quietly decline in the current unit test environment

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/BuiltinUiCaptureProviders.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaOffscreenCaptureProvider.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaFramebufferCaptureProvider.java Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java
git commit -m "feat: add builtin ui real capture providers"
```

### Task 5: Full verification

**Files:**
- Modify: none unless regressions are found

**Step 1: Run focused tests**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --tests "*UiCaptureArtifactStoreTest" --no-daemon`
Expected: PASS

**Step 2: Run full verification**

Run: `.\gradlew.bat :Mod:test :Server:test --no-daemon`
Expected: PASS

**Step 3: Inspect git status**

Run: `git status --short`
Expected: only intended files changed or clean after commit
