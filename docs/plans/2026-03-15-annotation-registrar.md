# Annotation Registrar Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace static/global MCP tool registration with annotation-scanned common/client/server registrar classes.

**Architecture:** ModDevMCP will use three side-specific registrar interfaces plus three side-specific annotations. A generic annotation lookup will read NeoForge scan metadata, filter by annotation before class loading, then instantiate registrar classes with public no-arg constructors. Common/client/server bootstrap paths will invoke only their own registrar type.

**Tech Stack:** Java, JUnit 5, NeoForge `ModList` / `ModFileScanData`, existing MCP tool provider APIs

---

### Task 1: Write failing tests for the new registrar model

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/BuiltinProviderRegistrationTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/registrar/AnnotationMcpRegistrarLookupTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/ModDevMcpRegistrarIntegrationTest.java`

**Step 1: Remove static-global registration assertions**

- Delete tests that depend on `ModDevMCP.registerTool(...)`.

**Step 2: Add lookup tests**

- Verify annotated registrar classes are discovered from synthetic scan data.
- Verify classes with the annotation but without the expected registrar interface are ignored.

**Step 3: Add integration tests**

- Verify common registrars are applied from the common bootstrap path.
- Verify client registrars are applied only from the client bootstrap path.
- Verify server registrars are applied only from the server bootstrap path.

### Task 2: Implement registrar annotations, interfaces, events, and lookup

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterCommonMcpToolsEvent.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterClientMcpToolsEvent.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterServerMcpToolsEvent.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/registrar/CommonMcpToolRegistrar.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/registrar/ClientMcpToolRegistrar.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/registrar/ServerMcpToolRegistrar.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/registrar/CommonMcpRegistrar.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/registrar/ClientMcpRegistrar.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/registrar/ServerMcpRegistrar.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/registrar/AnnotationMcpRegistrarLookup.java`

**Step 1: Add side-specific registrar contracts**

- Keep the API narrow and only expose `register(...)`.

**Step 2: Add generic annotation lookup**

- Use `ModList.get().getAllScanData()`.
- Filter by annotation metadata before loading classes.
- Instantiate only matching registrar implementations with a public no-arg constructor.

### Task 3: Integrate lookup into ModDevMCP bootstrap paths

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientRuntimeBootstrap.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ServerRuntimeBootstrap.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/ModMcpApi.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterMcpToolsEvent.java` if no longer used

**Step 1: Remove static global registration state**

- Delete the static cached tool provider registry and test reset hook.

**Step 2: Inject and run side-specific registrar lookups**

- Common bootstrap runs only common registrars.
- Client bootstrap runs only client registrars.
- Server bootstrap runs only server registrars.

**Step 3: Keep instance registration API**

- Preserve `ModMcpApi.registerToolProvider(...)` for direct instance-scoped registration.

### Task 4: Verify with focused Mod tests

**Files:**
- No additional source changes required

**Step 1: Run focused registrar tests**

- Run the registrar lookup and registrar integration tests.
- Run `BuiltinProviderRegistrationTest`.

**Step 2: Report exact results**

- If tests fail because of code, fix them.
- If they fail because of dependency, environment, or repository issues, report the exact failure point.
