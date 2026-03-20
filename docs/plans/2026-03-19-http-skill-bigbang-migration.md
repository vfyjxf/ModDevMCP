# HTTP Skill Bigbang Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete one-pass migration from legacy MCP/tool internals to operation+skill HTTP architecture while preserving product name `ModDevMCP`.

**Architecture:** Remove the runtime tool bridge and MCP protocol/stdio dispatch path, then make HTTP operation execution the only active invocation path. Replace tool-provider extension APIs with operation registration APIs and migrate naming/package semantics from `dev.vfyjxf.mcp` to `dev.vfyjxf.moddev`. Keep outdated artifacts only in `docs/outdated`, and delete outdated scripts under `tools/`.

**Tech Stack:** Java 21, NeoForge, Gradle, JUnit 5, local HTTP server, markdown skill export

---

### Task 1: Add migration guardrails (legacy usage detection)

**Files:**
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/LegacyMcpSurfaceGateTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/LegacyArchitectureCleanupTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/SkillFirstDocsTest.java`

**Step 1: Write the failing test**

```java
@Test
void production_code_must_not_reference_legacy_mcp_tool_surface() {
    var root = Path.of("Mod/src/main/java");
    var text = Files.walk(root)
            .filter(path -> path.toString().endsWith(".java"))
            .map(path -> {
                try {
                    return Files.readString(path);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            })
            .collect(Collectors.joining("\n"));

    assertFalse(text.contains("McpToolProvider"));
    assertFalse(text.contains("McpToolRegistry"));
    assertFalse(text.contains("ToolResult"));
    assertFalse(text.contains("ToolCallContext"));
    assertFalse(text.contains("McpProtocolDispatcher"));
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*LegacyMcpSurfaceGateTest" --no-daemon`
Expected: FAIL because current production code still references legacy MCP/tool APIs.

**Step 3: Write minimal implementation**

- Add/extend cleanup gate tests to scan production sources and skill docs.
- Keep failure messages explicit (include first matching token/path).

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*LegacyMcpSurfaceGateTest" --no-daemon`
Expected: PASS after full migration work is complete.

**Step 5: Commit**

```bash
git add Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/LegacyMcpSurfaceGateTest.java Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/LegacyArchitectureCleanupTest.java
git commit -m "test: add migration gate for legacy mcp surface"
```

### Task 2: Remove outdated assets (docs/tools/scripts)

**Files:**
- Delete: outdated files under `tools/` (legacy MCP/jsonrpc/bridge scripts)
- Move: outdated docs from `docs/plans/**` to `docs/outdated/plans/**`
- Modify: `README.md`
- Modify: `README.zh.md`

**Step 1: Write the failing test**

Add assertions in docs tests to fail when README mentions legacy MCP client bootstrap instructions.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*SkillFirstDocsTest" --tests "*HostArchitectureDocsTest" --no-daemon`
Expected: FAIL while stale wording remains.

**Step 3: Write minimal implementation**

- Delete obsolete scripts under `tools/` directly.
- Move old plans to `docs/outdated/plans`.
- Rewrite README sections to operation+skill HTTP only.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*SkillFirstDocsTest" --tests "*HostArchitectureDocsTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add tools docs/outdated README.md README.zh.md
git commit -m "chore: remove outdated assets and refresh top-level docs"
```

### Task 3: Replace runtime tool bridge with direct operation executors

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/OperationExecutorRegistry.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/RuntimeOperationBindings.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/UiOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/InputOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/CommandOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/WorldOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/HotswapOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/RequestsEndpoint.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/service/runtime/RuntimeOperationBindingsTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/runtime/OperationExecutorRegistryTest.java`

**Step 1: Write the failing test**

Create tests verifying operation execution uses executor registry directly and no tool-name lookup path exists.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*RuntimeOperationBindingsTest" --tests "*OperationExecutorRegistryTest" --tests "*OperationRequestEndpointTest" --no-daemon`
Expected: FAIL due current tool bridge.

**Step 3: Write minimal implementation**

- Introduce `OperationExecutorRegistry` keyed by `operationId`.
- Replace `ToolOperationInvoker` and `invokeTool(...)` flow with direct handlers.
- Map runtime errors to `OperationExecutionException` consistently.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*RuntimeOperationBindingsTest" --tests "*OperationExecutorRegistryTest" --tests "*OperationRequestEndpointTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/service/runtime Mod/src/main/java/dev/vfyjxf/mcp/service/http/RequestsEndpoint.java Mod/src/test/java/dev/vfyjxf/mcp/service/runtime Mod/src/test/java/dev/vfyjxf/mcp/service/http/OperationRequestEndpointTest.java
git commit -m "refactor: execute operations directly without tool bridge"
```

### Task 4: Migrate public extension API from tool providers to operation registrars

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/ModMcpApi.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java`
- Replace: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterMcpToolsEvent.java`
- Replace: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterCommonMcpToolsEvent.java`
- Replace: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterClientMcpToolsEvent.java`
- Replace: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterServerMcpToolsEvent.java`
- Replace: `Mod/src/main/java/dev/vfyjxf/mcp/api/registrar/CommonMcpToolRegistrar.java`
- Replace: `Mod/src/main/java/dev/vfyjxf/mcp/api/registrar/ClientMcpToolRegistrar.java`
- Replace: `Mod/src/main/java/dev/vfyjxf/mcp/api/registrar/ServerMcpToolRegistrar.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/ModDevMcpRegistrarIntegrationTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/registrar/AnnotationMcpRegistrarLookupTest.java`

**Step 1: Write the failing test**

Update registrar integration tests to assert operation registration events and APIs only.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*Registrar*" --tests "*ModDevMcpRegistrarIntegrationTest" --no-daemon`
Expected: FAIL while old tool-provider APIs remain.

**Step 3: Write minimal implementation**

- Replace provider sink with operation contributor sink.
- Replace registration events and registrar interfaces.
- Wire lookup/bootstrap to new interfaces.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*Registrar*" --tests "*ModDevMcpRegistrarIntegrationTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/api Mod/src/main/java/dev/vfyjxf/mcp/runtime Mod/src/test/java/dev/vfyjxf/mcp/registrar Mod/src/test/java/dev/vfyjxf/mcp/ModDevMcpRegistrarIntegrationTest.java
git commit -m "refactor: migrate extension registration to operation events"
```

### Task 5: Delete legacy MCP protocol and runtime tool layer

**Files:**
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/server/api/*.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/server/runtime/McpToolRegistry.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcher.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/*.java`
- Delete/Rewrite: legacy stdio bridge classes and tests under `Mod/src/main/java/dev/vfyjxf/mcp/server/**` and `Mod/src/test/java/dev/vfyjxf/mcp/server/**`

**Step 1: Write the failing test**

Add architecture cleanup assertions for deleted paths/tokens.

**Step 2: Run test to verify it fails**

Run: `rg -n "McpToolProvider|McpToolRegistry|ToolResult|ToolCallContext|McpProtocolDispatcher|tools/call|tools/list" Mod/src/main`
Expected: matches exist before cleanup.

**Step 3: Write minimal implementation**

Remove unused legacy classes and update imports/callers to operation path.

**Step 4: Run test to verify it passes**

Run: `rg -n "McpToolProvider|McpToolRegistry|ToolResult|ToolCallContext|McpProtocolDispatcher|tools/call|tools/list" Mod/src/main`
Expected: no matches.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/server Mod/src/main/java/dev/vfyjxf/mcp/runtime Mod/src/test/java/dev/vfyjxf/mcp/server
git commit -m "refactor: remove legacy mcp protocol and tool runtime layer"
```

### Task 6: Rename package namespace and symbols to `dev.vfyjxf.moddev`

**Files:**
- Move/Modify: `Mod/src/main/java/dev/vfyjxf/mcp/**` -> `Mod/src/main/java/dev/vfyjxf/moddev/**`
- Move/Modify: `Mod/src/test/java/dev/vfyjxf/mcp/**` -> `Mod/src/test/java/dev/vfyjxf/moddev/**`
- Modify: `TestMod/src/main/java/**` imports referencing old package
- Modify: `build.gradle`, `settings.gradle`, `Mod/build.gradle` where class names/packages are referenced

**Step 1: Write the failing test**

Add package contract test asserting no `package dev.vfyjxf.mcp` remains in active sources.

**Step 2: Run test to verify it fails**

Run: `rg -n "package dev\.vfyjxf\.mcp|import dev\.vfyjxf\.mcp" Mod/src/main Mod/src/test TestMod/src/main`
Expected: many matches.

**Step 3: Write minimal implementation**

- Move Java source trees.
- Rewrite package/import declarations.
- Rename MCP-specific symbols to operation/skill semantics.

**Step 4: Run test to verify it passes**

Run: `rg -n "package dev\.vfyjxf\.mcp|import dev\.vfyjxf\.mcp" Mod/src/main Mod/src/test TestMod/src/main`
Expected: no matches.

**Step 5: Commit**

```bash
git add Mod/src/main/java Mod/src/test/java TestMod/src/main/java build.gradle settings.gradle Mod/build.gradle
git commit -m "refactor: rename package namespace to dev.vfyjxf.moddev"
```

### Task 7: Update skills and guides to fully new operation flow

**Files:**
- Modify: `skills/moddev-usage/SKILL.md`
- Modify: `Mod/src/main/resources/moddev-service/skills/moddev-usage.md`
- Modify: `Mod/src/main/resources/moddev-service/categories/*.md`
- Modify: `docs/guides/*.md`
- Modify: `docs/guides/*.zh.md`

**Step 1: Write the failing test**

Add docs assertions that fail on legacy MCP client/jsonrpc/tool-provider terminology in active docs/skills.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*SkillFirstDocsTest" --tests "*LegacyArchitectureCleanupTest" --no-daemon`
Expected: FAIL before docs/skills rewrite.

**Step 3: Write minimal implementation**

Rewrite all skill/guides to canonical flow:
- probe: `GET /api/v1/status`
- discover: `/api/v1/categories`, `/api/v1/skills`, `/api/v1/operations`
- execute: `POST /api/v1/requests`

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*SkillFirstDocsTest" --tests "*LegacyArchitectureCleanupTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add skills Mod/src/main/resources/moddev-service docs/guides README.md README.zh.md
git commit -m "docs: align skills and guides with operation-first http architecture"
```

### Task 8: End-to-end verification and final migration commit set

**Files:**
- Verify all touched files in this migration

**Step 1: Run compile verification**

Run: `./gradlew.bat :Mod:compileJava :Mod:compileTestJava --no-daemon`
Expected: BUILD SUCCESSFUL.

**Step 2: Run test verification**

Run: `./gradlew.bat :Mod:test --no-daemon`
Expected: BUILD SUCCESSFUL.

**Step 3: Run static legacy token checks**

Run: `rg -n "McpToolProvider|McpToolRegistry|ToolResult|ToolCallContext|McpProtocolDispatcher|tools/call|tools/list|jsonrpc|stdio mcp" Mod/src/main README.md README.zh.md skills docs/guides`
Expected: no relevant matches outside `docs/outdated`.

**Step 4: Verify service-facing endpoints in tests/artifacts**

Run: `./gradlew.bat :Mod:test --tests "*OperationRequestEndpointTest" --tests "*SkillDiscoveryEndpointTest" --tests "*BuiltinSkillCatalogTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor: migrate from mcp tool layer to operation+skill http architecture"
```
