# UI Driver Composition Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** replace single-driver UI selection with a composition model that supports multiple active UI drivers per live screen, consistent driver filtering across UI tools, stable cross-driver target identity, and a clean separation between UI-semantic tools and raw input injection.

**Architecture:** add composition primitives on top of the existing driver registry, then route `UiToolProvider` through filtered active-driver sets instead of one selected driver. Keep existing single-driver behavior as the degenerate case, update session/ref identity to include `driverId`, and extend the existing `moddev.input_action` pipeline to own raw keyboard and mouse event injection while `moddev.ui_*` remains UI-semantic.

**Tech Stack:** Java 21, NeoForge client runtime, JUnit 5, existing `:Mod` Gradle test suite, existing MCP tool/provider/runtime abstractions.

---

### Task 1: Add Composition Primitives and Multi-Match Registry Support

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiDriverComposition.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiDriverCompositionResolver.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/UiDriverRegistry.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/UiDriverRegistryTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/UiDriverCompositionResolverTest.java`

**Step 1: Write the failing tests**

```java
@Test
void matchingDriversReturnsAllMatchesInPriorityOrder() {
    var registry = new UiDriverRegistry();
    registry.register(new TestUiDriver("base", 100, context -> true));
    registry.register(new TestUiDriver("addon", 300, context -> true));

    var matches = registry.matchingDrivers(contextWithHandle(new Object()));

    assertEquals(List.of("addon", "base"),
            matches.stream().map(driver -> driver.descriptor().id()).toList());
}

@Test
void compositionResolverKeepsDefaultDriverAsHighestPriorityMatch() {
    var composition = new UiDriverCompositionResolver(registry).resolve(contextWithHandle(handle));
    assertEquals("addon", composition.defaultDriverId());
    assertEquals(2, composition.drivers().size());
}
```

**Step 2: Run test to verify it fails**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*UiDriverRegistryTest\" --tests \"*UiDriverCompositionResolverTest\" --no-daemon"`

Expected: FAIL because `matchingDrivers(...)`, `UiDriverComposition`, and `UiDriverCompositionResolver` do not exist yet.

**Step 3: Write minimal implementation**

```java
public final class UiDriverRegistry {
    public List<UiDriver> matchingDrivers(UiContext context) {
        return drivers.stream().filter(driver -> driver.matches(context)).toList();
    }

    public Optional<UiDriver> select(UiContext context) {
        return matchingDrivers(context).stream().findFirst();
    }
}

public record UiDriverComposition(
        UiContext context,
        List<UiDriver> drivers,
        String defaultDriverId
) {
}
```

```java
public final class UiDriverCompositionResolver {
    public UiDriverComposition resolve(UiContext context) {
        var drivers = registry.matchingDrivers(context);
        var defaultDriverId = drivers.isEmpty() ? "" : drivers.getFirst().descriptor().id();
        return new UiDriverComposition(context, drivers, defaultDriverId);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*UiDriverRegistryTest\" --tests \"*UiDriverCompositionResolverTest\" --no-daemon"`

Expected: PASS with new multi-match behavior while existing `select(...)` still returns the first match.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/UiDriverRegistry.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiDriverComposition.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiDriverCompositionResolver.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/UiDriverRegistryTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/ui/UiDriverCompositionResolverTest.java
git commit -m "feat: add ui driver composition primitives"
```

### Task 2: Add Driver Filters and Aggregated Read-Only UI Tool Results

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/ui/UiSnapshot.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolProviderTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing tests**

```java
@Test
void liveScreenReportsAllActiveDrivers() {
    var result = tool.handler().handle(ToolCallContext.empty(), Map.of());
    var payload = cast(result.value());

    assertEquals(List.of("addon", "base"),
            ids((List<Map<String, Object>>) payload.get("drivers")));
    assertEquals("addon", payload.get("driverId"));
}

@Test
void snapshotCanExcludeSpecificDrivers() {
    var result = snapshotTool.handler().handle(ToolCallContext.empty(), Map.of(
            "excludeDrivers", List.of("addon")
    ));

    assertEquals(List.of("base:root"),
            targetKeys((List<Map<String, Object>>) cast(result.value()).get("targets")));
}

@Test
void queryAggregatesAcrossFilteredDrivers() {
    var result = queryTool.handler().handle(ToolCallContext.empty(), Map.of(
            "selector", Map.of("role", "button"),
            "includeDrivers", List.of("base", "addon")
    ));

    assertEquals(Set.of("base:play", "addon:pin"),
            targetKeys((List<Map<String, Object>>) cast(result.value()).get("targets")));
}
```

**Step 2: Run test to verify it fails**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*UiToolProviderTest\" --tests \"*UiToolInvocationTest\" --no-daemon"`

Expected: FAIL because `drivers`, `includeDrivers`, and `excludeDrivers` are not part of the current tool flow.

**Step 3: Write minimal implementation**

```java
private record DriverFilter(String driverId, Set<String> includeDrivers, Set<String> excludeDrivers) {
    boolean allows(UiDriver driver) {
        var id = driver.descriptor().id();
        if (driverId != null && !driverId.isBlank()) {
            return driverId.equals(id);
        }
        if (!includeDrivers.isEmpty() && !includeDrivers.contains(id)) {
            return false;
        }
        return !excludeDrivers.contains(id);
    }
}
```

```java
private UiDriverComposition filteredComposition(UiContext context, Map<String, Object> arguments) {
    var composition = compositionResolver.resolve(context);
    var filter = driverFilter(arguments);
    var filtered = composition.drivers().stream().filter(filter::allows).toList();
    return composition.withDrivers(filtered);
}
```

```java
return Map.of(
        "active", active,
        "screenClass", active ? metrics.screenClass() : "",
        "driverId", composition.defaultDriverId(),
        "drivers", composition.drivers().stream().map(this::driverMetadata).toList()
);
```

**Step 4: Run test to verify it passes**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*UiToolProviderTest\" --tests \"*UiToolInvocationTest\" --no-daemon"`

Expected: PASS with aggregated read-only UI results and backward-compatible `driverId`.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java Mod/src/main/java/dev/vfyjxf/mcp/api/ui/UiSnapshot.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolProviderTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "feat: add driver-filtered ui composition reads"
```

### Task 3: Make Session Refs and Action Routing Driver-Aware

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationRef.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManager.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManagerTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing tests**

```java
@Test
void sessionRefsRemainDistinctAcrossDriversWithSameTargetId() {
    var snapshot = snapshotWithTargets(
            target("base", "shared-id"),
            target("addon", "shared-id")
    );

    var session = manager.open(snapshot);

    assertEquals(2, session.refs().size());
    assertNotEquals(session.refs().get(0).refId(), session.refs().get(1).refId());
}

@Test
void actionFailsWhenMatchingTargetsSpanMultipleDriversWithoutExplicitDriver() {
    var result = actionTool.handler().handle(ToolCallContext.empty(), Map.of(
            "action", "click",
            "target", Map.of("id", "shared-id")
    ));

    assertFalse(result.success());
    assertEquals("target_ambiguous", result.error());
}
```

**Step 2: Run test to verify it fails**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*UiAutomationSessionManagerTest\" --tests \"*UiToolInvocationTest\" --no-daemon"`

Expected: FAIL because refs only track `targetId` and actions still assume one selected driver.

**Step 3: Write minimal implementation**

```java
public record UiAutomationRef(
        String refId,
        String driverId,
        String targetId,
        String screenId
) {
}
```

```java
var refKey = target.driverId() + "::" + target.targetId();
var existing = refsById.values().stream()
        .filter(refState -> !refState.stale() && refKey.equals(refState.ref().driverId() + "::" + refState.ref().targetId()))
        .findFirst()
        .orElse(null);
```

```java
if (matchingDrivers.size() > 1) {
    return ToolResult.failure("target_ambiguous");
}
return invokeSingleDriverAction(matchingDrivers.getFirst(), uiContext, request);
```

**Step 4: Run test to verify it passes**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*UiAutomationSessionManagerTest\" --tests \"*UiToolInvocationTest\" --no-daemon"`

Expected: PASS with driver-aware refs and explicit ambiguity failures.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationRef.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManager.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManagerTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "feat: make ui refs and actions driver-aware"
```

### Task 4: Separate UI-Semantic Input from Raw Input Injection

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/InputToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/InputActionDispatcher.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/InputCommand.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputController.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/LiveClientInputBridge.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouter.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputControllerTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouterTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/InputToolProviderTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`

**Step 1: Write the failing tests**

```java
@Test
void uiPressKeyFailsWhenNoActiveUiCompositionExists() {
    var result = uiPressKeyTool.handler().handle(ToolCallContext.empty(), Map.of("keyCode", 69));
    assertFalse(result.success());
    assertEquals("screen_unavailable", result.error());
}

@Test
void inputActionSupportsRawKeyDownAndKeyUpWithoutScreen() {
    var result = inputTool.handler().handle(ToolCallContext.empty(), Map.of(
            "action", "key_down",
            "keyCode", 341
    ));
    assertTrue(result.success());
}

@Test
void inputActionSupportsMouseDownMouseUpAndKeyClickWithModifiers() {
    assertTrue(dispatcher.dispatch("mouse_down", Map.of("button", 0)).success());
    assertTrue(dispatcher.dispatch("key_click", Map.of("keyCode", 88, "modifiers", GLFW.GLFW_MOD_CONTROL)).success());
}
```

**Step 2: Run test to verify it fails**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*MinecraftInputControllerTest\" --tests \"*KeyboardInputRouterTest\" --tests \"*InputToolProviderTest\" --tests \"*UiToolInvocationTest\" --no-daemon"`

Expected: FAIL because UI key tools still route through raw input and the raw input pipeline does not expose `key_down`, `key_up`, `mouse_down`, `mouse_up`, or `key_click`.

**Step 3: Write minimal implementation**

```java
// UiToolProvider
private ToolResult inputActionResult(String action, Map<String, Object> arguments) {
    var unavailable = unavailableScreenResult(arguments);
    if (unavailable != null) {
        return unavailable;
    }
    return rawInputActionResult(action, arguments);
}
```

```java
// InputToolProvider schema
Map.entry("action", Map.of("enum", List.of(
        "move", "hover", "click",
        "mouse_down", "mouse_up",
        "key_down", "key_up", "key_click",
        "type_text", "ui_intent"
)))
```

```java
// MinecraftInputController
command = switch (action) {
    case "key_down" -> keyDownCommand(arguments);
    case "key_up" -> keyUpCommand(arguments);
    case "key_click" -> keyClickCommand(arguments);
    case "mouse_down" -> mouseDownCommand(arguments, metrics);
    case "mouse_up" -> mouseUpCommand(arguments, metrics);
    default -> existingCommand(...);
};
```

**Step 4: Run test to verify it passes**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*MinecraftInputControllerTest\" --tests \"*KeyboardInputRouterTest\" --tests \"*InputToolProviderTest\" --tests \"*UiToolInvocationTest\" --no-daemon"`

Expected: PASS with raw input event primitives available through `moddev.input_action` and UI tools no longer falling through when no UI is active.

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/InputToolProvider.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/InputActionDispatcher.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/InputCommand.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputController.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/LiveClientInputBridge.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouter.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputControllerTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouterTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/InputToolProviderTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java
git commit -m "feat: separate raw input from ui semantics"
```

### Task 5: Update Built-In Docs and Third-Party Integration Guidance

**Files:**
- Modify: `docs/guides/2026-03-11-live-screen-tool-guide.md`
- Modify: `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- Modify: `docs/guides/2026-03-15-third-party-mod-integration-guide.md`
- Modify: `docs/guides/2026-03-11-live-screen-tool-guide.zh.md`
- Modify: `docs/guides/2026-03-12-playwright-style-ui-automation-guide.zh.md`
- Modify: `docs/guides/2026-03-15-third-party-mod-integration-guide.zh.md`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/LegacyArchitectureCleanupTest.java`

**Step 1: Write the failing doc assertions**

```java
@Test
void architectureDocsMentionDriverFilteringAndRawInputBoundary() {
    var content = Files.readString(Path.of("docs/guides/2026-03-15-third-party-mod-integration-guide.md"));
    assertTrue(content.contains("includeDrivers"));
    assertTrue(content.contains("excludeDrivers"));
    assertTrue(content.contains("moddev.input_action"));
}
```

**Step 2: Run test to verify it fails**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*LegacyArchitectureCleanupTest\" --no-daemon"`

Expected: FAIL because the guides still describe single-driver selection and do not document the raw-input boundary clearly.

**Step 3: Write minimal documentation updates**

```md
- `moddev.ui_get_live_screen` now returns `drivers[]` for all active UI drivers and keeps `driverId` as the default driver.
- UI tools accept `driverId`, `includeDrivers`, and `excludeDrivers`.
- Use `moddev.input_action` for raw key/mouse event injection that should bypass UI-driver semantics.
```

**Step 4: Run test to verify it passes**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*LegacyArchitectureCleanupTest\" --no-daemon"`

Expected: PASS with docs aligned to the new UI composition and input model.

**Step 5: Commit**

```bash
git add docs/guides/2026-03-11-live-screen-tool-guide.md docs/guides/2026-03-12-playwright-style-ui-automation-guide.md docs/guides/2026-03-15-third-party-mod-integration-guide.md docs/guides/2026-03-11-live-screen-tool-guide.zh.md docs/guides/2026-03-12-playwright-style-ui-automation-guide.zh.md docs/guides/2026-03-15-third-party-mod-integration-guide.zh.md Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/LegacyArchitectureCleanupTest.java
git commit -m "docs: explain ui driver composition and raw input"
```

### Task 6: Run Full Regression Suite for the Changed Areas

**Files:**
- Verify only: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/UiDriverRegistryTest.java`
- Verify only: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputControllerTest.java`
- Verify only: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouterTest.java`
- Verify only: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/InputToolProviderTest.java`
- Verify only: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiAutomationSessionManagerTest.java`
- Verify only: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolInvocationTest.java`
- Verify only: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolProviderTest.java`

**Step 1: Run the targeted regression suite**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests \"*UiDriverRegistryTest\" --tests \"*UiDriverCompositionResolverTest\" --tests \"*MinecraftInputControllerTest\" --tests \"*KeyboardInputRouterTest\" --tests \"*InputToolProviderTest\" --tests \"*UiAutomationSessionManagerTest\" --tests \"*UiToolInvocationTest\" --tests \"*UiToolProviderTest\" --no-daemon"`

Expected: PASS.

**Step 2: Run the module test suite**

Run: `pwsh -NoProfile -Command "$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --no-daemon"`

Expected: PASS with no regressions in unrelated runtime or doc tests.

**Step 3: Inspect git diff for accidental contract drift**

Run: `pwsh -NoProfile -Command "git diff --stat HEAD~5..HEAD"`

Expected: only UI composition, input, tests, and guide files changed.

**Step 4: Write final verification notes**

```md
- multiple active drivers are reported
- driver filtering works across observation tools
- cross-driver refs remain stable
- raw input works without active UI
- ui tools fail cleanly when no UI composition exists
```

**Step 5: Commit any final fixups**

```bash
git add .
git commit -m "test: verify ui driver composition rollout"
```
