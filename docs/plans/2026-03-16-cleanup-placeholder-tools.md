# Placeholder Tool Cleanup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove placeholder event and inventory tool exposure, along with the unused inventory driver extension surface and stale docs/tests.

**Architecture:** Keep the runtime event publisher and registrar event flow intact, but stop registering public event placeholder tools. Remove the inventory placeholder tool path end-to-end, including runtime registration, API hooks, and documentation that advertises unsupported inventory automation.

**Tech Stack:** Java, JUnit 5, Gradle, Markdown docs

---

### Task 1: Remove placeholder runtime classes and registrations

**Files:**
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/EventToolProvider.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/InventoryToolProvider.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/inventory/VanillaInventoryDriver.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/InventoryDriver.java`
- Delete: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/InventoryDriverRegistry.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientRuntimeBootstrap.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/ModMcpApi.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/event/RegisterClientMcpToolsEvent.java`

### Task 2: Update tests to reflect removed capabilities

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/BuiltinProviderRegistrationTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/bridge/LegacyStdioHostBridgeCompatibilityTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/ModDevMcpRegistrarIntegrationTest.java`

### Task 3: Clean docs that advertise removed tools or APIs

**Files:**
- Modify: `README.md`
- Modify: `README.zh.md`
- Modify: `docs/guides/2026-03-15-third-party-mod-integration-guide.md`
- Modify: `docs/guides/2026-03-15-third-party-mod-integration-guide.zh.md`

### Task 4: Verify targeted compilation and tests

**Verification:**
- Run: `.\\gradlew.bat :Mod:test --tests "*BuiltinProviderRegistrationTest" --tests "*LegacyStdioHostBridgeCompatibilityTest" --tests "*ModDevMcpRegistrarIntegrationTest" --no-daemon`
