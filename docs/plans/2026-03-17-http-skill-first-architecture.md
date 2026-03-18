# HTTP Skill-First Architecture Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the current gateway/tool-list-first architecture with a Mod-only local HTTP service that serves built-in skills and executes runtime operations through a generic request API.

**Architecture:** `Mod` becomes the single runtime product and owns service config, skill metadata, skill markdown, operation dispatch, HTTP endpoints, and filesystem export. The new service reuses existing runtime logic where practical, but the external contract shifts to `status`, `skills`, `categories`, `operations`, and `requests`, with `targetSide` resolved only when omission is unambiguous.

**Tech Stack:** Java 21, NeoForge, JUnit 5, existing mod runtime services, JDK built-in HTTP server or equivalent loopback HTTP support, Gradle

---

### Task 1: Lock in the new product boundary and terminology with failing tests

**Files:**
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/SkillFirstDocsTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/HostArchitectureDocsTest.java`
- Modify: `settings.gradle`
- Modify: `README.md`
- Modify: `README.zh.md`

**Step 1: Write the failing test**

- Add assertions that the primary architecture is a local HTTP service inside `Mod`.
- Add assertions that the public docs reference `moddev-usage`, `/api/v1/status`, and exported skills.
- Add assertions that root build docs no longer describe `:Server` or `:Plugin` as required end-user products.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*SkillFirstDocsTest" --tests "*HostArchitectureDocsTest" --no-daemon`
Expected: FAIL because the docs and build still describe the old gateway/client-config flow.

**Step 3: Write minimal implementation**

- Update the root module list and top-level docs to describe the new final architecture.
- Keep the code untouched for now; this task only pins the migration direction.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*SkillFirstDocsTest" --tests "*HostArchitectureDocsTest" --no-daemon`
Expected: PASS

### Task 2: Add the service config, metadata contracts, and registries in `Mod`

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/config/ServiceConfig.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/category/CategoryDefinition.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/skill/SkillDefinition.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/skill/SkillKind.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/operation/OperationDefinition.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/operation/OperationRegistry.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/skill/SkillRegistry.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/config/ServiceConfigTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/skill/SkillRegistryTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/operation/OperationRegistryTest.java`

**Step 1: Write the failing tests**

- Verify default host, port, and export-root resolution.
- Verify `moddev-usage` is mandatory.
- Verify `guidance`, `action`, and `hybrid` rules for `operationId`.
- Verify operation metadata exposes `supportsTargetSide` and explicit category ownership.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*ServiceConfigTest" --tests "*SkillRegistryTest" --tests "*OperationRegistryTest" --no-daemon`
Expected: FAIL because none of the new service contracts exist.

**Step 3: Write minimal implementation**

- Add immutable config/metadata records or value objects.
- Keep field names explicit: `skillId`, `categoryId`, `operationId`, `requestId`, `targetSide`, `input`, `errorCode`, `errorMessage`.
- Keep registry APIs read-only after bootstrap.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*ServiceConfigTest" --tests "*SkillRegistryTest" --tests "*OperationRegistryTest" --no-daemon`
Expected: PASS

### Task 3: Add the HTTP discovery and status surface

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/HttpServiceServer.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/HttpJson.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/StatusEndpoint.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/CategoriesEndpoint.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/SkillsEndpoint.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/OperationsEndpoint.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/http/ServiceStatusEndpointTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/http/SkillDiscoveryEndpointTest.java`

**Step 1: Write the failing tests**

- Verify `GET /api/v1/status` returns `serviceReady`, `gameReady`, `connectedSides`, `entrySkillId`, `exportRoot`, and `lastError`.
- Verify `GET /api/v1/categories`, `GET /api/v1/skills`, and `GET /api/v1/operations` return stable JSON metadata.
- Verify `GET /api/v1/skills/{skillId}/markdown` returns markdown instead of JSON.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*ServiceStatusEndpointTest" --tests "*SkillDiscoveryEndpointTest" --no-daemon`
Expected: FAIL because the HTTP surface does not exist.

**Step 3: Write minimal implementation**

- Stand up a loopback-only HTTP server.
- Return JSON for discovery/status and raw markdown for skill bodies.
- Keep endpoint registration centralized so future categories/skills stay declarative.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*ServiceStatusEndpointTest" --tests "*SkillDiscoveryEndpointTest" --no-daemon`
Expected: PASS

### Task 4: Add generic request execution and correct `targetSide` resolution

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/request/OperationRequest.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/request/OperationResponse.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/request/OperationError.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/TargetSideResolver.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/RequestsEndpoint.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/request/TargetSideResolverTest.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/http/OperationRequestEndpointTest.java`

**Step 1: Write the failing tests**

- Verify `targetSide` is rejected for operations that do not support side selection.
- Verify omission auto-resolves when exactly one eligible side is connected.
- Verify omission returns `target_side_required` when both client and server can handle the operation.
- Verify execution failures return structured `errorCode` and `errorMessage` instead of a fake disconnect result.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*TargetSideResolverTest" --tests "*OperationRequestEndpointTest" --no-daemon`
Expected: FAIL because the current routing logic lives in old tool/gateway code and does not expose the required HTTP error contract.

**Step 3: Write minimal implementation**

- Implement the generic `POST /api/v1/requests` envelope.
- Centralize target-side resolution in one resolver.
- Keep the response format identical for success and failure except for `status` plus error fields.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*TargetSideResolverTest" --tests "*OperationRequestEndpointTest" --no-daemon`
Expected: PASS

### Task 5: Build the built-in skill catalog and bundled markdown resources

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/skill/BuiltinSkillCatalog.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/skill/SkillMarkdownLoader.java`
- Create: `Mod/src/main/resources/moddev-service/skills/moddev-usage.md`
- Create: `Mod/src/main/resources/moddev-service/categories/status.md`
- Create: `Mod/src/main/resources/moddev-service/categories/ui.md`
- Create: `Mod/src/main/resources/moddev-service/categories/command.md`
- Create: `Mod/src/main/resources/moddev-service/categories/world.md`
- Create: `Mod/src/main/resources/moddev-service/categories/hotswap.md`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/skill/BuiltinSkillCatalogTest.java`

**Step 1: Write the failing tests**

- Verify `moddev-usage` is always present.
- Verify category skills and operation skills resolve markdown from bundled resources.
- Verify guidance-only skills can exist without an operation.
- Verify action/hybrid skills embed the correct operation id and `curl` examples.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*BuiltinSkillCatalogTest" --no-daemon`
Expected: FAIL because the built-in skill catalog and markdown resources do not exist.

**Step 3: Write minimal implementation**

- Store markdown as resources, not generated source.
- Keep the entry skill focused on discovery plus request conventions.
- Keep category markdown short and operation-specific markdown factual.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*BuiltinSkillCatalogTest" --no-daemon`
Expected: PASS

### Task 6: Export the skill projection to a fixed local directory

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/export/SkillExportService.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/export/SkillExportLayout.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/export/SkillExportServiceTest.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/SkillsEndpoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/HttpServiceServer.java`

**Step 1: Write the failing tests**

- Verify startup export writes `manifest.json`, `skills/moddev-usage.md`, per-skill markdown, per-category markdown, and index files.
- Verify `POST /api/v1/skills/export` regenerates the tree.
- Verify export content is derived from the in-memory registries and not loaded back from disk.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*SkillExportServiceTest" --tests "*SkillDiscoveryEndpointTest" --no-daemon`
Expected: FAIL because no export service exists.

**Step 3: Write minimal implementation**

- Export on startup.
- Regenerate the full tree on refresh instead of doing partial patching.
- Keep the export root configurable for tests but fixed by default in production.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*SkillExportServiceTest" --tests "*SkillDiscoveryEndpointTest" --no-daemon`
Expected: PASS

### Task 7: Map existing runtime capabilities onto operations inside `Mod`

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/RuntimeOperationBindings.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/UiOperationHandlers.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/CommandOperationHandlers.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/WorldOperationHandlers.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/runtime/HotswapOperationHandlers.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ServerEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/UiToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/CommandToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/WorldToolProvider.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/HotswapToolProvider.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/runtime/RuntimeOperationBindingsTest.java`

**Step 1: Write the failing tests**

- Verify the new operation ids cover the existing user-visible capability areas.
- Verify side-aware operations bind to the correct runtime services.
- Verify hotswap failures now return structured execution errors instead of a generic disconnect.

**Step 2: Run test to verify it fails**

Run: `./gradlew.bat :Mod:test --tests "*RuntimeOperationBindingsTest" --tests "*HotswapToolProviderTest" --tests "*CommandToolProviderTest" --no-daemon`
Expected: FAIL because the runtime logic is not yet exposed through the new operation layer.

**Step 3: Write minimal implementation**

- Reuse existing runtime services where possible.
- Keep provider classes only as short-lived adapters if they are still needed during migration.
- Move the public contract to operation ids and request payloads.

**Step 4: Run test to verify it passes**

Run: `./gradlew.bat :Mod:test --tests "*RuntimeOperationBindingsTest" --tests "*HotswapToolProviderTest" --tests "*CommandToolProviderTest" --no-daemon`
Expected: PASS

### Task 8: Delete the obsolete standalone modules and old client-config flow

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle`
- Modify: `Mod/build.gradle`
- Delete: `Server/build.gradle`
- Delete: `Server/src/main/java/dev/vfyjxf/mcp/server/...`
- Delete: `Server/src/test/java/dev/vfyjxf/mcp/server/...`
- Delete: `Plugin/build.gradle`
- Delete: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/...`
- Delete: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/...`
- Modify: `TestMod/build.gradle`
- Modify: `TestMod/settings.gradle`

**Step 1: Write the failing test**

- Use text-search checks to pin removal of the standalone module wiring and generated client-config flow.

**Step 2: Run test to verify it fails**

Run: `rg -n ":Server|:Plugin|runStdioMcp|createMcpClientFiles|mcp-gateway|mcp-backend|mcpServers" settings.gradle build.gradle README.md README.zh.md Mod Plugin Server TestMod docs skills -g "*.gradle" -g "*.md" -g "*.java"`
Expected: matches found before cleanup.

**Step 3: Write minimal implementation**

- Remove `Server` and `Plugin` from the build.
- Remove their published-artifact docs and test scaffolding.
- Switch `TestMod` from generated client-config guidance to local service + exported skill guidance.

**Step 4: Run verification**

Run: `rg -n ":Server|:Plugin|runStdioMcp|createMcpClientFiles|mcp-gateway|mcp-backend|mcpServers" settings.gradle build.gradle README.md README.zh.md Mod Plugin Server TestMod docs skills -g "*.gradle" -g "*.md" -g "*.java"`
Expected: only intentional historical-plan references remain

### Task 9: Update repo skills, guides, and final verification

**Files:**
- Modify: `skills/moddevmcp-usage/SKILL.md`
- Modify: `skills/moddevmcp-usage/agents/openai.yaml`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.md`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.zh.md`
- Modify: `docs/guides/2026-03-11-agent-preflight-checklist.md`
- Modify: `docs/guides/2026-03-11-agent-preflight-checklist.zh.md`
- Modify: `docs/guides/2026-03-11-testmod-runclient-guide.md`
- Modify: `docs/guides/2026-03-11-testmod-runclient-guide.zh.md`
- Modify: `docs/plans/2026-03-17-http-skill-first-architecture-design.md`
- Modify: `docs/plans/2026-03-17-http-skill-first-architecture.md`

**Step 1: Write the failing test**

- Add doc assertions or text-search checks that repo skills and guides teach `curl` against the local service, mention exported skills, and use the `targetSide` rule correctly.

**Step 2: Run test to verify it fails**

Run: `rg -n "moddev-usage|/api/v1/status|/api/v1/requests|targetSide|curl" skills docs/guides README.md README.zh.md -g "*.md" -g "*.yaml"`
Expected: missing or outdated matches before cleanup.

**Step 3: Write minimal implementation**

- Rewrite the usage skill around service discovery and request execution.
- Make sure entry guidance does not require old client-config installation.
- Document that some exported skills are guidance-only.

**Step 4: Run test to verify it passes**

Run: `rg -n "moddev-usage|/api/v1/status|/api/v1/requests|targetSide|curl" skills docs/guides README.md README.zh.md -g "*.md" -g "*.yaml"`
Expected: the new flow is documented consistently

### Task 10: Run end-to-end verification from `Mod` and `TestMod`

**Files:**
- Verify generated/exported output under the default skill export root and the `TestMod` run directory

**Step 1: Run focused module tests**

Run: `./gradlew.bat :Mod:test --tests "*ServiceConfigTest" --tests "*SkillRegistryTest" --tests "*ServiceStatusEndpointTest" --tests "*OperationRequestEndpointTest" --tests "*BuiltinSkillCatalogTest" --tests "*SkillExportServiceTest" --tests "*RuntimeOperationBindingsTest" --no-daemon`
Expected: PASS

**Step 2: Run full `Mod` tests**

Run: `./gradlew.bat :Mod:test --no-daemon`
Expected: PASS

**Step 3: Run a real `TestMod` game session**

Run: `./gradlew.bat -p TestMod runClient --no-daemon`
Expected: PASS long enough to confirm the local HTTP service starts and exports skills

**Step 4: Probe the running service manually**

Run: `curl http://127.0.0.1:47812/api/v1/status`
Expected: JSON response with `serviceReady=true`

**Step 5: Probe entry skill markdown manually**

Run: `curl http://127.0.0.1:47812/api/v1/skills/moddev-usage/markdown`
Expected: markdown body for the entry skill

