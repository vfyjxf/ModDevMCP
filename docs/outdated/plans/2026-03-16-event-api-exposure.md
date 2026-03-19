# Event API Exposure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make side-specific MCP registrar events expose `ModMcpApi` and the appropriate runtime registration helpers so downstream integrations no longer need a direct `ModDevMCP` instance.

**Architecture:** Extend the existing `RegisterMcpToolsEvent` base class so it owns the tool provider sink, `ModMcpApi`, and the runtime event publisher. Keep the current `register(McpToolProvider)` compatibility method, add explicit helper aliases, then let client/common/server event subclasses expose only the helper surface that belongs on that side.

**Tech Stack:** Java 21, NeoForge-side runtime API, JUnit 5

---

### Task 1: Add failing integration tests for the new event surface

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/ModDevMcpRegistrarIntegrationTest.java`

**Step 1: Write failing tests**

- Add a client-side registrar integration test that uses the client event to:
  - register a tool directly
  - register a tool through `event.api()`
  - register UI driver / inventory driver / input controller / interaction resolver / offscreen provider / framebuffer provider
  - publish an event
- Add a common or server test that verifies `eventPublisher()` and `registerToolProvider(...)` are available.

**Step 2: Run the focused test to confirm RED**

Run: `.\gradlew.bat :Mod:test --tests "*ModDevMcpRegistrarIntegrationTest" --no-daemon`

Expected:
- FAIL because the event classes do not yet expose `api()`, `eventPublisher()`, or the client helper methods

### Task 2: Implement the expanded event surface

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterMcpToolsEvent.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterCommonMcpToolsEvent.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterClientMcpToolsEvent.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterServerMcpToolsEvent.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`

**Step 1: Extend the base event**

- Store `ModMcpApi` and `EventPublisher`
- Add `api()`, `eventPublisher()`, `publishEvent(...)`, and `registerToolProvider(...)`
- Keep `register(McpToolProvider)` as a compatibility alias

**Step 2: Extend side-specific subclasses**

- Client event gets the client runtime helper methods
- Common and server events stay narrower

**Step 3: Pass the new dependencies from `ModDevMCP`**

- Construct each event with the mod API and event publisher

### Task 3: Re-run focused tests and broader regression tests

**Files:**
- Verify: `Mod/src/test/java/dev/vfyjxf/mcp/ModDevMcpRegistrarIntegrationTest.java`
- Verify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/BuiltinProviderRegistrationTest.java`
- Verify: `Mod/src/test/java/dev/vfyjxf/mcp/registrar/AnnotationMcpRegistrarLookupTest.java`
- Verify: `Mod/src/test/java/dev/vfyjxf/mcp/IntegratedServerRuntimeHostTest.java`

**Step 1: Run focused registrar tests**

Run: `.\gradlew.bat :Mod:test --tests "*ModDevMcpRegistrarIntegrationTest" --no-daemon`

Expected:
- PASS

**Step 2: Run the broader regression slice**

Run: `.\gradlew.bat :Mod:test --tests "*BuiltinProviderRegistrationTest" --tests "*AnnotationMcpRegistrarLookupTest" --tests "*ModDevMcpRegistrarIntegrationTest" --tests "*IntegratedServerRuntimeHostTest" --no-daemon`

Expected:
- PASS

### Task 4: Update docs if needed

**Files:**
- Modify: `docs/guides/2026-03-15-third-party-mod-integration-guide.md`
- Modify: `docs/guides/2026-03-15-third-party-mod-integration-guide.zh.md`

**Step 1: Update the integration guide**

- Document that the registrar event itself now exposes `api()` and direct helper methods
- Clarify that runtime adapter registration can now happen inside registrar callbacks without a direct `ModDevMCP` instance
