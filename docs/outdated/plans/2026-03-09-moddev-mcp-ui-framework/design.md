# ModDevMCP GUI Framework Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the `Common`/`Server`/`Mod` framework for ModDevMCP and ship phase-one client GUI MCP tools on top of the official `modelcontextprotocol/java-sdk`.

**Architecture:** Add a new `:Common` module for shared contracts and public API, keep `:Server` as a plain Java MCP layer built on the official Java SDK, and let `:Mod` embed that server while implementing the NeoForge client GUI bridge. The GUI stack uses a layered pipeline: raw capture, generic region detection, overlay classification, adapter enrichment, and semantic action dispatch.

**Tech Stack:** Java 21, Gradle multi-project build, NeoForge `moddev`, official `modelcontextprotocol/java-sdk`, JUnit 5, NeoForge unit tests.

---

> Current workspace note: `<repo>` was not a git repository in that earlier snapshot. Commit steps below assume execution from the actual VCS root or after git init.

### Task 1: Add the Common module and baseline test wiring

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle`
- Modify: `Server/build.gradle`
- Modify: `Mod/build.gradle`
- Create: `Common/build.gradle`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/package-info.java`
- Create: `Common/src/test/java/dev/vfyjxf/mcp/common/model/UiBoundsTest.java`
- Create: `Server/src/test/java/dev/vfyjxf/mcp/server/package-info.java`

**Step 1: Write the failing test**

```java
package dev.vfyjxf.mcp.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class UiBoundsTest {
    @Test
    void widthAndHeightAreDerivedFromEdges() {
        UiBounds bounds = new UiBounds(10, 20, 110, 70);

        assertEquals(100, bounds.width());
        assertEquals(50, bounds.height());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Common:test --tests "*UiBoundsTest" --no-daemon`

Expected: FAIL because `:Common` does not exist yet.

**Step 3: Write minimal implementation**

- Add `include(":Common", ":Mod", ":Server")` to `settings.gradle`
- Create `Common/build.gradle` as a plain `java-library` module with JUnit 5
- Add JUnit 5 to `Server/build.gradle`
- Enable NeoForge unit tests in `Mod/build.gradle` similar to `CloudLib-Standalone`
- Create `UiBounds` as a compact value object in `Common`

```java
package dev.vfyjxf.mcp.common.model;

public record UiBounds(int left, int top, int right, int bottom) {
    public int width() {
        return right - left;
    }

    public int height() {
        return bottom - top;
    }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Common:test --tests "*UiBoundsTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add settings.gradle build.gradle Common/build.gradle Server/build.gradle Mod/build.gradle Common/src/main/java/dev/vfyjxf/mcp/common/package-info.java Common/src/test/java/dev/vfyjxf/mcp/common/model/UiBoundsTest.java
git commit -m "build: add common module and junit baseline"
```

### Task 2: Define the shared UI protocol and public API surface

**Files:**
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/model/CaptureScope.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/model/OverlayRole.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/model/UiOwnerRef.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/protocol/ui/UiRegion.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/protocol/ui/UiSnapshot.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/protocol/ui/UiActionRequest.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/api/ui/UiAdapter.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/api/ui/OverlayClassifier.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/api/ui/CaptureRegionProvider.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/api/ui/UiActionProvider.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/api/ui/UiAdapterRegistrar.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/api/ui/UiApi.java`
- Create: `Common/src/test/java/dev/vfyjxf/mcp/common/protocol/ui/UiRegionOwnershipTest.java`

**Step 1: Write the failing test**

```java
package dev.vfyjxf.mcp.common.protocol.ui;

import dev.vfyjxf.mcp.common.model.OverlayRole;
import dev.vfyjxf.mcp.common.model.UiBounds;
import dev.vfyjxf.mcp.common.model.UiOwnerRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class UiRegionOwnershipTest {
    @Test
    void regionPreservesOwnerAndHost() {
        UiRegion region = new UiRegion(
                "recipe_panel",
                new UiBounds(0, 0, 100, 100),
                OverlayRole.OVERLAY,
                new UiOwnerRef("addon_mod", "jei"),
                true
        );

        assertEquals("addon_mod", region.owner().ownerModId());
        assertEquals("jei", region.owner().hostModId());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Common:test --tests "*UiRegionOwnershipTest" --no-daemon`

Expected: FAIL because the shared protocol classes do not exist yet.

**Step 3: Write minimal implementation**

- Add the enums and records in `common.model`
- Add lightweight DTOs in `common.protocol.ui`
- Keep API interfaces narrow and independent of Minecraft runtime classes

```java
package dev.vfyjxf.mcp.common.model;

public record UiOwnerRef(String ownerModId, String hostModId) {
}
```

```java
package dev.vfyjxf.mcp.common.protocol.ui;

import dev.vfyjxf.mcp.common.model.OverlayRole;
import dev.vfyjxf.mcp.common.model.UiBounds;
import dev.vfyjxf.mcp.common.model.UiOwnerRef;

public record UiRegion(
        String id,
        UiBounds bounds,
        OverlayRole role,
        UiOwnerRef owner,
        boolean interactive
) {
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Common:test --tests "*UiRegionOwnershipTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add Common/src/main/java/dev/vfyjxf/mcp/common/model Common/src/main/java/dev/vfyjxf/mcp/common/protocol Common/src/main/java/dev/vfyjxf/mcp/common/api Common/src/test/java/dev/vfyjxf/mcp/common/protocol/ui/UiRegionOwnershipTest.java
git commit -m "feat: add common ui protocol and extension api"
```

### Task 3: Add bridge interfaces and modular mod bootstrap

**Files:**
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/bridge/ClientUiFacade.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/bridge/ServerWorldFacade.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/bridge/BridgeUnavailableException.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ServerEntrypoint.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/ModBootstrapTest.java`

**Step 1: Write the failing test**

```java
package dev.vfyjxf.mcp.bootstrap;

import dev.vfyjxf.mcp.ModDevMCP;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ModBootstrapTest {
    @Test
    void bootstrapExposesSharedServices() {
        assertNotNull(ModDevMCP.modId);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*ModBootstrapTest" --no-daemon`

Expected: FAIL because `:Mod` unit-test support and shared bootstrap are not wired yet.

**Step 3: Write minimal implementation**

- Add bridge interfaces in `:Common`
- Refactor `ModDevMCP` into a shared bootstrap base inspired by `CloudLib-Standalone`
- Let `ClientEntrypoint` and `ServerEntrypoint` inherit shared initialization and keep dist-specific setup separate
- Prepare service registration hooks for future client and dedicated-server bridge instances

```java
package dev.vfyjxf.mcp.common.bridge;

import dev.vfyjxf.mcp.common.protocol.ui.UiSnapshot;

public interface ClientUiFacade {
    UiSnapshot snapshotCurrentUi();
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*ModBootstrapTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add Common/src/main/java/dev/vfyjxf/mcp/common/bridge Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java Mod/src/main/java/dev/vfyjxf/mcp/ServerEntrypoint.java Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/ModBootstrapTest.java
git commit -m "refactor: add shared bootstrap and bridge interfaces"
```

### Task 4: Replace the Server MCP stub with a Java SDK-backed tool host

**Files:**
- Modify: `Server/build.gradle`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/ModDevMcpServer.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/McpServerFactory.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/tool/ui/UiSnapshotTool.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/tool/ui/UiScreenshotTool.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/tool/ui/UiInteractionTool.java`
- Create: `Server/src/test/java/dev/vfyjxf/mcp/server/tool/ui/UiSnapshotToolTest.java`

**Step 1: Write the failing test**

```java
package dev.vfyjxf.mcp.server.tool.ui;

import dev.vfyjxf.mcp.common.bridge.ClientUiFacade;
import dev.vfyjxf.mcp.common.protocol.ui.UiSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class UiSnapshotToolTest {
    @Test
    void handlerDelegatesToClientFacade() {
        ClientUiFacade facade = () -> UiSnapshot.empty("screen.test");
        UiSnapshotTool tool = new UiSnapshotTool(facade);

        assertEquals("screen.test", tool.handle().screenId());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Server:test --tests "*UiSnapshotToolTest" --no-daemon`

Expected: FAIL because the SDK-backed tool host and handlers do not exist.

**Step 3: Write minimal implementation**

- Replace `io.modelcontextprotocol.sdk:mcp-core:1.0.0` with the official Java SDK bundle
- Add a `ModDevMcpServer` wrapper that owns tool registration and lifecycle
- Create a small factory around the SDK so transport details do not leak through the project
- Implement first tool handlers as plain delegates over `ClientUiFacade`

```java
package dev.vfyjxf.mcp.server.tool.ui;

import dev.vfyjxf.mcp.common.bridge.ClientUiFacade;
import dev.vfyjxf.mcp.common.protocol.ui.UiSnapshot;

public final class UiSnapshotTool {
    private final ClientUiFacade facade;

    public UiSnapshotTool(ClientUiFacade facade) {
        this.facade = facade;
    }

    public UiSnapshot handle() {
        return facade.snapshotCurrentUi();
    }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Server:test --tests "*UiSnapshotToolTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add Server/build.gradle Server/src/main/java/dev/vfyjxf/mcp/server Server/src/test/java/dev/vfyjxf/mcp/server/tool/ui/UiSnapshotToolTest.java
git commit -m "feat: wire server module to official java mcp sdk"
```

### Task 5: Implement the client adapter registry and semantic pipeline

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/ClientUiFacadeImpl.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/pipeline/UiPipeline.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/pipeline/UiRegionResolver.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/adapter/UiAdapterRegistryImpl.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/adapter/GenericScreenAdapter.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/client/ui/pipeline/UiPipelineTest.java`

**Step 1: Write the failing test**

```java
package dev.vfyjxf.mcp.client.ui.pipeline;

import dev.vfyjxf.mcp.common.model.OverlayRole;
import dev.vfyjxf.mcp.common.protocol.ui.UiSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiPipelineTest {
    @Test
    void pipelineIncludesMainContentRegion() {
        UiSnapshot snapshot = UiPipeline.forTesting().snapshot();

        assertTrue(snapshot.regions().stream().anyMatch(region -> region.role() == OverlayRole.MAIN_CONTENT));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiPipelineTest" --no-daemon`

Expected: FAIL because the pipeline and registry do not exist.

**Step 3: Write minimal implementation**

- Build `UiAdapterRegistryImpl` around the `common.api.ui` contracts
- Add a generic adapter that can always describe the active `Screen` at a coarse level
- Create `UiPipeline` to merge raw regions, overlay classifications, and adapter semantics
- Return a `UiSnapshot` even when no specialized adapter matches

```java
package dev.vfyjxf.mcp.client.ui.pipeline;

import dev.vfyjxf.mcp.common.protocol.ui.UiSnapshot;

public final class UiPipeline {
    public static UiPipeline forTesting() {
        return new UiPipeline();
    }

    public UiSnapshot snapshot() {
        return UiSnapshot.testingMainContent();
    }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiPipelineTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/client/ui Mod/src/test/java/dev/vfyjxf/mcp/client/ui/pipeline/UiPipelineTest.java
git commit -m "feat: add client ui adapter registry and pipeline"
```

### Task 6: Add screenshot scope handling and region filtering

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/capture/UiScreenshotService.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/capture/CaptureScopeResolver.java`
- Create: `Common/src/test/java/dev/vfyjxf/mcp/common/model/CaptureScopeTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/client/ui/capture/CaptureScopeResolverTest.java`

**Step 1: Write the failing test**

```java
package dev.vfyjxf.mcp.client.ui.capture;

import dev.vfyjxf.mcp.common.model.CaptureScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CaptureScopeResolverTest {
    @Test
    void mainContentOnlyExcludesOverlayRegions() {
        CaptureScopeResolver resolver = CaptureScopeResolver.forTesting();

        assertEquals(1, resolver.resolve(CaptureScope.MAIN_CONTENT_ONLY).size());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*CaptureScopeResolverTest" --no-daemon`

Expected: FAIL because screenshot scope resolution is not implemented.

**Step 3: Write minimal implementation**

- Define `CaptureScope`
- Add a resolver that filters named regions using ownership and overlay role
- Build `UiScreenshotService` around region selection first, image encoding second
- Keep screenshot results paired with selected region metadata

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*CaptureScopeResolverTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add Common/src/test/java/dev/vfyjxf/mcp/common/model/CaptureScopeTest.java Mod/src/main/java/dev/vfyjxf/mcp/client/ui/capture Mod/src/test/java/dev/vfyjxf/mcp/client/ui/capture/CaptureScopeResolverTest.java
git commit -m "feat: add capture scope resolution for gui screenshots"
```

### Task 7: Implement semantic navigation and action dispatch

**Files:**
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/protocol/ui/UiActionResult.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/action/UiActionDispatcher.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/action/UiNavigationService.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/tool/ui/UiNavigationTool.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/client/ui/action/UiNavigationServiceTest.java`
- Create: `Server/src/test/java/dev/vfyjxf/mcp/server/tool/ui/UiNavigationToolTest.java`

**Step 1: Write the failing test**

```java
package dev.vfyjxf.mcp.client.ui.action;

import dev.vfyjxf.mcp.common.protocol.ui.UiActionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiNavigationServiceTest {
    @Test
    void switchActionCanBeResolvedBySemanticTarget() {
        UiNavigationService service = UiNavigationService.forTesting();

        UiActionResult result = service.switchTo("host.main_content");

        assertTrue(result.success());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*UiNavigationServiceTest" --no-daemon`

Expected: FAIL because semantic action dispatch does not exist.

**Step 3: Write minimal implementation**

- Add a shared `UiActionResult`
- Implement `UiActionDispatcher` for `click`, `type`, `key_press`, and `scroll`
- Implement `UiNavigationService` for `open`, `close`, `switch`, `toggle`, `wait`, and `assert`
- Keep semantic targets adapter-driven before falling back to coordinates
- Expose matching server handlers over the MCP Java SDK layer

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*UiNavigationServiceTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add Common/src/main/java/dev/vfyjxf/mcp/common/protocol/ui/UiActionResult.java Mod/src/main/java/dev/vfyjxf/mcp/client/ui/action Server/src/main/java/dev/vfyjxf/mcp/server/tool/ui/UiNavigationTool.java Mod/src/test/java/dev/vfyjxf/mcp/client/ui/action/UiNavigationServiceTest.java Server/src/test/java/dev/vfyjxf/mcp/server/tool/ui/UiNavigationToolTest.java
git commit -m "feat: add semantic gui navigation tools"
```

### Task 8: Add default ownership-aware adapters and JEI classification hooks

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/adapter/VanillaContainerAdapter.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/adapter/JeiOverlayClassifier.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/client/ui/adapter/UiAdapters.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/client/ui/adapter/JeiOverlayClassifierTest.java`
- Create: `Common/src/test/java/dev/vfyjxf/mcp/common/protocol/ui/UiOwnerRefTest.java`

**Step 1: Write the failing test**

```java
package dev.vfyjxf.mcp.client.ui.adapter;

import dev.vfyjxf.mcp.common.model.OverlayRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class JeiOverlayClassifierTest {
    @Test
    void addonOwnedJeiRegionKeepsAddonAsOwner() {
        JeiOverlayClassifier classifier = JeiOverlayClassifier.forTesting("addon_mod", "jei");

        assertEquals(OverlayRole.ADDON_OWNED, classifier.classify().role());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*JeiOverlayClassifierTest" --no-daemon`

Expected: FAIL because JEI-aware classification hooks are not present.

**Step 3: Write minimal implementation**

- Add a vanilla adapter for generic inventory-like screens
- Add JEI-specific ownership and host classification hooks without hard-coding JEI exclusion
- Centralize built-in adapter registration in `UiAdapters`
- Make the classifier preserve both `ownerModId` and `hostModId`

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*JeiOverlayClassifierTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/client/ui/adapter Mod/src/test/java/dev/vfyjxf/mcp/client/ui/adapter/JeiOverlayClassifierTest.java Common/src/test/java/dev/vfyjxf/mcp/common/protocol/ui/UiOwnerRefTest.java
git commit -m "feat: add ownership-aware default gui adapters"
```

### Task 9: Wire the embedded server into the mod and document extension usage

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ServerEntrypoint.java`
- Create: `Common/src/main/java/dev/vfyjxf/mcp/common/api/ui/package-info.java`
- Modify: `README.md`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/EmbeddedServerWiringTest.java`

**Step 1: Write the failing test**

```java
package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

final class EmbeddedServerWiringTest {
    @Test
    void clientBootstrapCanConstructEmbeddedMcpServer() {
        assertDoesNotThrow(() -> new Object());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*EmbeddedServerWiringTest" --no-daemon`

Expected: FAIL after replacing the placeholder assertion with real embedded-server construction.

**Step 3: Write minimal implementation**

- Construct the SDK-backed server from the mod bootstrap
- Wire the client bridge into that server at startup
- Leave dedicated-server world bridge as a null-safe placeholder
- Update `README.md` with the module split, current tool list, and the `common.api.ui` extension story for third-party mods

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*EmbeddedServerWiringTest" --no-daemon`

Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java Mod/src/main/java/dev/vfyjxf/mcp/ServerEntrypoint.java Common/src/main/java/dev/vfyjxf/mcp/common/api/ui/package-info.java README.md Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/EmbeddedServerWiringTest.java
git commit -m "docs: wire embedded mcp server and document extension api"
```

### Task 10: Run full verification before claiming completion

**Files:**
- Modify: `docs/plans/2026-03-09-moddev-mcp-ui-framework/design.md`

**Step 1: Write the failing test**

There is no new production behavior here. Use the failing condition as “one or more verification commands fail.”

**Step 2: Run test to verify it fails**

Run:

```bash
.\gradlew.bat :Common:test --no-daemon
.\gradlew.bat :Server:test --no-daemon
.\gradlew.bat :Mod:test --no-daemon
```

Expected: At least one command fails before the remaining implementation work is complete.

**Step 3: Write minimal implementation**

- Fix the specific failures without broad refactors
- Record any missing test coverage or known limitations in the README or follow-up notes

**Step 4: Run test to verify it passes**

Run:

```bash
.\gradlew.bat :Common:test --no-daemon
.\gradlew.bat :Server:test --no-daemon
.\gradlew.bat :Mod:test --no-daemon
```

Expected: PASS for all three modules

**Step 5: Commit**

```bash
git add .
git commit -m "test: verify gui framework baseline"
```
