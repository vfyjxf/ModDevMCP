# Operation API Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the legacy MCP tool/provider execution layer with a single operation-based runtime and migrate all built-in plus extension-facing registrations to that model.

**Architecture:** Keep the existing HTTP service surface, but replace the internal tool bridge with direct operation definitions and executors. Built-in runtime domains register operations directly, and the public extension API switches from tool-provider registration to operation registration events and registrars.

**Tech Stack:** Java 21, NeoForge runtime code, custom HTTP service endpoints, operation metadata/dispatch registries, JUnit 5

---

### Task 1: Add failing tests for direct operation execution

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/service/runtime/RuntimeOperationBindingsTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/runtime/OperationExecutorRegistryTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/service/http/OperationRequestEndpointTest.java`

**Step 1: Write the failing test**

Add tests that assert:
- built-in operations execute without any tool lookup dependency
- UI capture is exposed as an operation, not only as a tool
- request execution no longer needs `ToolResult`

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*RuntimeOperationBindingsTest" --tests "*OperationExecutorRegistryTest" --tests "*OperationRequestEndpointTest" --no-daemon`

Expected: FAIL because the runtime still routes requests through legacy tool abstractions.

**Step 3: Write minimal implementation**

Introduce a direct executor registry and wire request execution to it.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*RuntimeOperationBindingsTest" --tests "*OperationExecutorRegistryTest" --tests "*OperationRequestEndpointTest" --no-daemon`

Expected: PASS

### Task 2: Replace built-in tool providers with operation contributors

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientRuntimeBootstrap.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ServerRuntimeBootstrap.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/UiOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/InputOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/CommandOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/WorldOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/StatusOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/HotswapOperationHandlers.java`

**Step 1: Write the failing test**

Extend runtime tests to assert:
- bootstrap does not instantiate legacy tool providers
- all currently supported public operations are present after bootstrap
- `ui.capture` becomes part of the HTTP operation set

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*RuntimeOperationBindingsTest" --tests "*BuiltinSkillCatalogTest" --tests "*SkillDiscoveryEndpointTest" --no-daemon`

Expected: FAIL because bootstrap and operation assembly still depend on `*ToolProvider`.

**Step 3: Write minimal implementation**

Move execution logic out of `runtime/tool/*ToolProvider` classes into direct operation handlers or shared runtime services.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*RuntimeOperationBindingsTest" --tests "*BuiltinSkillCatalogTest" --tests "*SkillDiscoveryEndpointTest" --no-daemon`

Expected: PASS

### Task 3: Migrate the public extension API from tools to operations

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

**Step 1: Write the failing test**

Add or update API tests so they assert:
- operation registrars are discoverable
- old tool registrar types are gone
- extensions can register operation definitions and executors through the public API

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*Registrar*" --tests "*ModMcpApi*" --no-daemon`

Expected: FAIL because the public API still exposes tool-provider registration.

**Step 3: Write minimal implementation**

Introduce operation registrar interfaces and events, then update discovery and bootstrap wiring.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*Registrar*" --tests "*ModMcpApi*" --no-daemon`

Expected: PASS

### Task 4: Delete the legacy tool and stdio execution layer

**Files:**
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/server/api/McpToolProvider.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/server/api/McpToolHandler.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/server/api/ToolResult.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/server/api/ToolCallContext.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/server/runtime/McpToolRegistry.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcher.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/*.java`
- Delete or rewrite: old stdio/bootstrap classes and tests that only exist for `tools/call`

**Step 1: Write the failing test**

Add architecture cleanup assertions that fail if any legacy tool-layer references remain in production sources.

**Step 2: Run test to verify it fails**

Run: `rg -n "McpToolProvider|McpToolRegistry|ToolResult|ToolCallContext|tools/call|tools/list" Mod/src/main`

Expected: matches still exist.

**Step 3: Write minimal implementation**

Delete remaining tool-layer code and update callers to the operation path.

**Step 4: Run test to verify it passes**

Run: `rg -n "McpToolProvider|McpToolRegistry|ToolResult|ToolCallContext|tools/call|tools/list" Mod/src/main`

Expected: no matches in active production code.

### Task 5: Verify HTTP discovery, skills, and real runtime behavior

**Files:**
- Modify: `README.md`
- Modify: `README.zh.md`
- Modify: `skills/moddev-usage/SKILL.md`
- Modify: `Mod/src/main/resources/moddev-service/skills/moddev-usage.md`
- Modify: `Mod/src/main/resources/moddev-service/categories/ui.md`
- Modify: `gradle.properties`
- Modify: `TestMod/gradle.properties`

**Step 1: Write the failing test**

Add documentation and skill assertions that:
- no longer mention tool-provider registration
- describe the operation-first runtime correctly
- include the now-migrated capture capability

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*BuiltinSkillCatalogTest" --tests "*SkillFirstDocsTest" --tests "*LegacyArchitectureCleanupTest" --no-daemon`

Expected: FAIL until docs and exported skill text match the new architecture.

**Step 3: Write minimal implementation**

Update public docs and bump version to `0.2.3`.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*BuiltinSkillCatalogTest" --tests "*SkillFirstDocsTest" --tests "*LegacyArchitectureCleanupTest" --no-daemon`

Expected: PASS

### Task 6: Run end-to-end verification and publish

**Files:**
- Verify generated output under: `Mod/run/build/moddevmcp`
- Publish: local Maven artifacts

**Step 1: Run targeted compile and regression**

Run: `.\gradlew.bat :Mod:test --no-daemon`

Expected: PASS

**Step 2: Run real runtime verification**

Run the client, verify:
- `GET /api/v1/operations` includes all migrated operations
- `input.action` can open inventory
- `ui.capture` can execute through HTTP and honor excluded targets

Expected: PASS with real capture output artifact.

**Step 3: Publish**

Run: `.\gradlew.bat publishToMavenLocal --no-daemon`

Expected: `BUILD SUCCESSFUL`
