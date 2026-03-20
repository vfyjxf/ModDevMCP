# Virtual Modifier State Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** add persistent agent-owned modifier state so explicit modifier holds survive across later input events and are visible to vanilla and NeoForge modifier queries.

**Architecture:** Introduce a small `VirtualModifierState` utility in the input runtime, route all injected events through a single merged modifier view, and patch vanilla `Screen` plus NeoForge `KeyModifier` query helpers through a narrow client-side Mixin configuration. Keep `key_click` and `key_press` modifiers request-scoped while making explicit modifier `key_down` and `key_up` persistent.

**Tech Stack:** Java 21, NeoForge 21.1.x, Sponge Mixin, JUnit 5, Gradle

---

### Task 1: Add failing tests for virtual modifier state

**Files:**
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierStateTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouterTest.java`

**Step 1: Write the failing test**

```java
@Test
void leftAndRightShiftShareOneLogicalState() {
    var state = new VirtualModifierState();

    state.keyDown(GLFW.GLFW_KEY_LEFT_SHIFT);
    assertTrue(state.shiftActive());

    state.keyUp(GLFW.GLFW_KEY_RIGHT_SHIFT);
    assertFalse(state.shiftActive());
}
```

```java
@Test
void explicitModifierHoldAppliesToLaterPlainKeyEvents() {
    var state = new VirtualModifierState();
    var fallback = new RecordingFallbackInput();

    state.keyDown(GLFW.GLFW_KEY_LEFT_SHIFT);
    KeyboardInputRouter.keyDown(command(GLFW.GLFW_KEY_A, 0), fallback, state);

    assertEquals(List.of("down:65:1"), fallback.events);
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*VirtualModifierStateTest" --tests "*KeyboardInputRouterTest" --no-daemon`
Expected: FAIL because `VirtualModifierState` and the new router overload do not exist yet.

**Step 3: Write minimal implementation**

- Create `VirtualModifierState` with logical modifier tracking, left/right key normalization, bitmask export, and `clear()`
- Add the minimal router hooks needed for tests to compile
- Add concise Javadoc on the new state class and any non-obvious router helper

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*VirtualModifierStateTest" --tests "*KeyboardInputRouterTest" --no-daemon`
Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierStateTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouterTest.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierState.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouter.java
git commit -m "feat: add virtual modifier state core"
```

### Task 2: Route live input through merged modifier state

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/LiveClientInputBridge.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouter.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/ModifiedKeybindingDispatch.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/ModifiedKeybindingDispatchTest.java`

**Step 1: Write the failing test**

```java
@Test
void dispatchUsesPersistentVirtualModifiersForMatchingBindings() {
    var state = new VirtualModifierState();
    state.keyDown(GLFW.GLFW_KEY_LEFT_CONTROL);
    var binding = new RecordingBinding(true);
    var dispatch = new ModifiedKeybindingDispatch(() -> List.of(binding), state::modifierBits);

    assertTrue(dispatch.dispatch(GLFW.GLFW_KEY_Y, 0, () -> {}));
}
```

```java
@Test
void oneShotClickModifiersDoNotPersistAfterDispatch() {
    var state = new VirtualModifierState();
    var fallback = new RecordingFallbackInput();

    KeyboardInputRouter.keyClick(command(GLFW.GLFW_KEY_A, GLFW.GLFW_MOD_SHIFT), fallback, state);
    KeyboardInputRouter.keyDown(command(GLFW.GLFW_KEY_B, 0), fallback, state);

    assertEquals(List.of("down:340:1", "down:65:1", "up:65:1", "up:340:1", "down:66:0"), fallback.events);
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*ModifiedKeybindingDispatchTest" --tests "*KeyboardInputRouterTest" --no-daemon`
Expected: FAIL because live dispatch still relies on raw command modifiers only.

**Step 3: Write minimal implementation**

- Inject `VirtualModifierState` into the live client keyboard path
- Compute effective modifier bits as persistent virtual bits OR one-shot command bits
- Update explicit modifier `key_down` / `key_up` handling so the persistent state changes before later events observe it
- Keep `key_click` / `key_press` modifiers request-scoped
- Add concise Javadoc where the merged-modifier behavior is not obvious

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*ModifiedKeybindingDispatchTest" --tests "*KeyboardInputRouterTest" --no-daemon`
Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/LiveClientInputBridge.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouter.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/ModifiedKeybindingDispatch.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/ModifiedKeybindingDispatchTest.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouterTest.java
git commit -m "feat: merge virtual modifiers into input dispatch"
```

### Task 3: Cover controller-level command semantics and reset behavior

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputController.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputControllerTest.java`

**Step 1: Write the failing test**

```java
@Test
void keyClickModifiersRemainRequestScopedAtControllerBoundary() {
    var bridge = new RecordingClientInputBridge(metrics(), OperationResult.success(null));
    var controller = new MinecraftInputController(bridge, new UiPointerStateRegistry(), intent -> -1);

    controller.perform("key_click", Map.of("keyCode", 88, "modifiers", GLFW.GLFW_MOD_SHIFT));
    controller.perform("key_down", Map.of("keyCode", 65));

    assertEquals(GLFW.GLFW_MOD_SHIFT, bridge.recordedCommands().get(0).modifiers());
    assertEquals(0, bridge.recordedCommands().get(1).modifiers());
}
```

```java
@Test
void clientStartupClearsStaleVirtualModifierState() {
    VirtualModifierState.global().keyDown(GLFW.GLFW_KEY_LEFT_SHIFT);

    VirtualModifierState.global().clear();

    assertEquals(0, VirtualModifierState.global().modifierBits());
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*MinecraftInputControllerTest" --tests "*VirtualModifierStateTest" --no-daemon`
Expected: FAIL because controller and startup lifecycle do not document or enforce the new boundary yet.

**Step 3: Write minimal implementation**

- Keep controller payload semantics unchanged: explicit `key_down` / `key_up` represent persistent modifier operations, while command `modifiers` stay one-shot
- Add an explicit clear on a stable client lifecycle boundary such as client startup and shutdown
- Add short Javadocs or comments on the lifecycle reset and controller semantics where needed

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*MinecraftInputControllerTest" --tests "*VirtualModifierStateTest" --no-daemon`
Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputController.java Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputControllerTest.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierState.java
git commit -m "feat: define controller and lifecycle modifier semantics"
```

### Task 4: Add modifier query mixins for vanilla and NeoForge

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/mixin/client/ScreenModifierMixin.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/mixin/client/KeyModifierMixin.java`
- Create: `Mod/src/main/resources/moddevmcp.mixins.json`
- Modify: `Mod/src/main/templates/META-INF/neoforge.mods.toml`
- Modify: `Mod/build.gradle`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierQueriesTest.java`

**Step 1: Write the failing test**

```java
@Test
void screenQueryHelperReturnsTrueWhenVirtualShiftIsActive() {
    assertTrue(VirtualModifierQueries.merge(false, true));
}
```

```java
@Test
void controlQueryIncludesVirtualSuperOnMacStylePaths() {
    assertTrue(VirtualModifierQueries.controlActive(false, false, true, true));
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*VirtualModifierQueriesTest" --no-daemon`
Expected: FAIL because the query helper and mixin bootstrap do not exist yet.

**Step 3: Write minimal implementation**

- Create a small helper such as `VirtualModifierQueries` to hold testable merge logic
- Add mixins that delegate to the helper after reading the original method result
- Add the narrow Mixin configuration needed for the client mod to load these patches
- Add Javadocs on the helper and brief comments in the mixins if the injection point is non-obvious

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*VirtualModifierQueriesTest" --no-daemon`
Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/mixin/client/ScreenModifierMixin.java Mod/src/main/java/dev/vfyjxf/mcp/mixin/client/KeyModifierMixin.java Mod/src/main/resources/moddevmcp.mixins.json Mod/src/main/templates/META-INF/neoforge.mods.toml Mod/build.gradle Mod/src/test/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierQueriesTest.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierQueries.java
git commit -m "feat: expose virtual modifiers to query helpers"
```

### Task 5: Run the full regression slice and polish comments

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/LiveClientInputBridge.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouter.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/ModifiedKeybindingDispatch.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputController.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierState.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierQueries.java`

**Step 1: Review comments and Javadocs**

- Add or tighten Javadocs on every new public or package-visible helper introduced by this feature
- Keep comments only where the modifier lifecycle or Mixin behavior is genuinely non-obvious

**Step 2: Run focused regression tests**

Run: `.\gradlew.bat :Mod:test --tests "*VirtualModifierStateTest" --tests "*KeyboardInputRouterTest" --tests "*ModifiedKeybindingDispatchTest" --tests "*MinecraftInputControllerTest" --tests "*VirtualModifierQueriesTest" --no-daemon`
Expected: PASS

**Step 3: Run the full module test suite**

Run: `.\gradlew.bat :Mod:test --no-daemon`
Expected: PASS

**Step 4: Verify the mod compiles with mixins enabled**

Run: `.\gradlew.bat :Mod:compileJava --no-daemon`
Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/LiveClientInputBridge.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/KeyboardInputRouter.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/ModifiedKeybindingDispatch.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/MinecraftInputController.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierState.java Mod/src/main/java/dev/vfyjxf/mcp/runtime/input/VirtualModifierQueries.java Mod/src/main/java/dev/vfyjxf/mcp/mixin/client/ScreenModifierMixin.java Mod/src/main/java/dev/vfyjxf/mcp/mixin/client/KeyModifierMixin.java
git commit -m "docs: clarify virtual modifier behavior"
```
