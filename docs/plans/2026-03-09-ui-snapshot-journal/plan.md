# UI Snapshot Journal Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** add runtime snapshot refs and pluggable default interaction-state resolution to the built-in UI MCP tools.

**Architecture:** add two small runtime services in `Mod`: an in-memory `UiSnapshotJournal` and a registry-driven `UiInteractionStateResolver` SPI. `UiToolProvider` records pre and post snapshots through the journal, while built-in UI drivers ask a resolver for default focused and selected targets before applying session overrides.

**Tech Stack:** Java 21, Gradle, JUnit 5, existing `Server` plus `Mod` modules

---

### Task 1: Snapshot Journal Red-Green

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiSnapshotJournal.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing test**

Add tests asserting:
- `ui_capture` returns non-empty `snapshotRef`
- `ui_capture` returns `capturedSnapshot.driverId`
- `ui_action` returns `preSnapshotRef` and `postSnapshotRef`

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because snapshot refs are missing from tool payloads.

**Step 3: Write minimal implementation**

Implement an in-memory journal that:
- generates refs like `ui-1`
- stores the full `UiSnapshot`
- records the latest snapshot per current UI context

Wire it into `RuntimeRegistries`.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS for the new snapshot-ref assertions.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiSnapshotJournal.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/RuntimeRegistries.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "feat: add ui snapshot journal"
```

### Task 2: Interaction Resolver SPI Red-Green

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiInteractionStateResolver.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/UiInteractionStateResolverRegistry.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/BuiltinUiInteractionResolvers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/ModMcpApi.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaScreenUiDriver.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaContainerUiDriver.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/FallbackRegionUiDriver.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing test**

Add tests asserting:
- `ui_open` restores the resolver-defined default focused target
- `ui_close` clears the post-action snapshot
- `ui_switch` post snapshot reflects the switched target

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because drivers still own default interaction rules directly and tool payloads do not expose the post-action snapshot.

**Step 3: Write minimal implementation**

Create a resolver SPI and registry, register built-in resolvers during mod bootstrap, and update built-in drivers to ask the registry for default ids before applying per-session overrides.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS for the new resolver-driven behavior.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiInteractionStateResolver.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/UiInteractionStateResolverRegistry.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/BuiltinUiInteractionResolvers.java Mod/src/main/java/dev/vfyjxf/mcp/api/ModMcpApi.java Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaScreenUiDriver.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/VanillaContainerUiDriver.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/FallbackRegionUiDriver.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "feat: add ui interaction resolver spi"
```

### Task 3: Tool Integration And Verification

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing test**

Add assertions that:
- `ui_capture` returns `capturedSnapshot`
- `ui_action` and semantic action wrappers return `postActionSnapshot`
- snapshot refs are non-empty and differ when state changes

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: FAIL because `UiToolProvider` does not currently expose these fields.

**Step 3: Write minimal implementation**

Update `UiToolProvider` to:
- capture pre and post snapshots
- record them in the journal
- include snapshot refs and structured snapshots in tool payloads

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "feat: expose ui snapshot refs in tool payloads"
```

### Task 4: Full Verification

**Files:**
- Modify: none unless a regression is found

**Step 1: Run focused module tests**

Run: `.\gradlew.bat :Mod:test --tests "*UiToolInvocationTest" --no-daemon`
Expected: PASS

**Step 2: Run full project tests**

Run: `.\gradlew.bat :Mod:test :Server:test --no-daemon`
Expected: PASS

**Step 3: Inspect git status**

Run: `git status --short`
Expected: only intended files changed or clean after commit

**Step 4: Commit final integration if needed**

```bash
git add ...
git commit -m "feat: finalize ui snapshot journal integration"
```
