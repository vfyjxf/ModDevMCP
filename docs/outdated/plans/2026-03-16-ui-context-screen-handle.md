# UiContext Screen Handle Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add `screenHandle()` to `UiContext` and wire live client contexts so custom `UiDriver` implementations can match against the real screen instance without binding the public API to `Screen`.

**Architecture:** Extend the existing `UiContext` contract with a null-safe weakly typed handle, then update the live UI tool path to populate it from the active client screen. Keep registry selection and existing built-in drivers intact, and add a small helper plus focused tests so downstream handle-based drivers can be implemented safely.

**Tech Stack:** Java 21, NeoForge/Minecraft client runtime, JUnit 5, Gradle, Markdown docs

---

### Task 1: Add failing tests for the new `UiContext` contract

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolProviderTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/api/runtime/UiContextTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/UiDriverRegistryTest.java`

**Step 1: Write the failing compatibility test for `UiContext`**

Add a test in `Mod/src/test/java/dev/vfyjxf/mcp/api/runtime/UiContextTest.java` that creates a minimal anonymous `UiContext` implementation and asserts:

```java
@Test
void screenHandleDefaultsToNull() {
    UiContext context = new UiContext() {
        @Override
        public String screenClass() {
            return "example.Screen";
        }
    };

    assertNull(context.screenHandle());
}
```

**Step 2: Write the failing driver-selection test**

Add a test in `Mod/src/test/java/dev/vfyjxf/mcp/runtime/UiDriverRegistryTest.java` with:

- a high-priority fake driver that returns `true` only when `context.screenHandle()` is a marker object
- a lower-priority fallback fake driver that always matches

Assert that the registry selects the handle-aware driver when the marker object is present.

**Step 3: Write the failing live-context propagation test**

Add a focused test in `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/UiToolProviderTest.java` around whatever helper or internal path currently creates `UiContext`, and assert that the created context carries a non-null handle when a live screen is present.

**Step 4: Run tests to verify they fail**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.api.runtime.UiContextTest --tests dev.vfyjxf.mcp.runtime.UiDriverRegistryTest --tests dev.vfyjxf.mcp.runtime.tool.UiToolProviderTest --no-daemon --rerun-tasks`

Expected:

- `UiContextTest` fails because `screenHandle()` does not exist yet
- `UiDriverRegistryTest` fails because the contract is missing
- `UiToolProviderTest` fails because no live handle is propagated yet

### Task 2: Extend the `UiContext` API and add a handle helper

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiContext.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiContextScreenHandles.java`

**Step 1: Add the minimal API surface**

Update `Mod/src/main/java/dev/vfyjxf/mcp/api/runtime/UiContext.java` with:

```java
default Object screenHandle() {
    return null;
}
```

Do not remove or change existing methods.

**Step 2: Add a helper utility for null-safe handle access**

Create `Mod/src/main/java/dev/vfyjxf/mcp/runtime/ui/UiContextScreenHandles.java` with a small utility API such as:

```java
public final class UiContextScreenHandles {

    private UiContextScreenHandles() {
    }

    public static Object raw(UiContext context) {
        return context == null ? null : context.screenHandle();
    }

    public static <T> T as(UiContext context, Class<T> type) {
        Object handle = raw(context);
        return type.isInstance(handle) ? type.cast(handle) : null;
    }
}
```

Keep this helper intentionally small.

**Step 3: Run focused tests**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.api.runtime.UiContextTest --tests dev.vfyjxf.mcp.runtime.UiDriverRegistryTest --no-daemon --rerun-tasks`

Expected: `UiContextTest` passes; other tests may still fail until live propagation is implemented.

### Task 3: Populate the live screen handle in UI tool contexts

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`

**Step 1: Find every internal `UiContext` implementation used by the UI tool path**

Inspect the local records/helpers in `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`, especially the record currently used for live-screen and probe-driven selection.

**Step 2: Extend those context implementations to carry a handle**

Update the relevant context record(s) so they include:

```java
Object screenHandle
```

and implement:

```java
@Override
public Object screenHandle() {
    return screenHandle;
}
```

**Step 3: Populate the handle from the live client screen**

When creating the `UiContext` for active runtime operations, pass the real current screen object into the context. Preserve current behavior when no active screen exists by passing `null`.

**Step 4: Keep lightweight/probe-only contexts null-safe**

For contexts created only from metadata, such as light selection paths that only know the class name, explicitly pass `null` as the handle.

**Step 5: Run focused tests**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.runtime.tool.UiToolProviderTest --tests dev.vfyjxf.mcp.runtime.UiDriverRegistryTest --no-daemon --rerun-tasks`

Expected: PASS

### Task 4: Align built-in docs and extension guidance

**Files:**
- Modify: `docs/guides/2026-03-15-third-party-mod-integration-guide.md`
- Modify: `README.md`

**Step 1: Fix the third-party integration example**

Update the guide so the `UiDriver` example no longer claims `context.screen()` exists. Replace it with a `screenHandle()`-based example and note that client runtime contexts typically expose the active `Screen` as the handle.

**Step 2: Add a short compatibility note**

Document:

- `screenHandle()` may be `null`
- built-in drivers still work without using it
- custom drivers should treat it as an optional live runtime handle

**Step 3: Update the root README only if it mentions outdated driver examples**

Keep changes minimal and avoid duplicating the guide.

### Task 5: Verify end-to-end compilation and targeted behavior

**Files:**
- Modify: `docs/plans/2026-03-16-ui-context-screen-handle-design.md`
- Modify: `docs/plans/2026-03-16-ui-context-screen-handle.md`

**Step 1: Run the focused test suite**

Run: `gradlew.bat :Mod:test --tests dev.vfyjxf.mcp.api.runtime.UiContextTest --tests dev.vfyjxf.mcp.runtime.UiDriverRegistryTest --tests dev.vfyjxf.mcp.runtime.tool.UiToolProviderTest --no-daemon --rerun-tasks`

Expected: `BUILD SUCCESSFUL`

**Step 2: Run compile verification**

Run: `gradlew.bat :Mod:compileJava --no-daemon`

Expected: `BUILD SUCCESSFUL`

**Step 3: Record the real verification outcome**

Append the actual commands and results to this plan or to an adjacent implementation note before claiming completion.
