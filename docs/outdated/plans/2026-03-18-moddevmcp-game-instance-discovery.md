# ModDevMCP Game Instance Discovery Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add project-local game instance discovery so agents can resolve the active ModDevMCP `baseUrl` reliably when client and server run on separate ports.

**Architecture:** Keep the fast-path default probe at `http://127.0.0.1:47812`, but make the runtime publish project-local discovery records at `<gradleProject>/build/moddevmcp/game-instances.json`. Treat the registry as a candidate list only; agents and helpers still confirm liveness through `/api/v1/status`, then route requests by supported sides and `targetSide`.

**Tech Stack:** Java 21, built-in HTTP server, JUnit 5, NeoForge run configuration, Markdown skill docs.

---

### Task 1: Define project-local discovery config and registry format

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/config/ServiceConfig.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/service/config/ServiceConfigTest.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/discovery/GameInstanceRecord.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/service/discovery/GameInstanceRegistry.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/discovery/GameInstanceRegistryTest.java`

**Step 1: Write the failing tests**

Add assertions that `ServiceConfig` can derive the project-local registry path under `build/moddevmcp/game-instances.json`, and add registry tests for:
- writing `client` and `server` entries
- overwriting a side entry
- removing a side entry
- preserving valid JSON structure

Example registry assertion:

```java
assertEquals(
        projectRoot.resolve("build/moddevmcp/game-instances.json").toAbsolutePath().normalize(),
        config.gameInstancesPath()
);
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew.bat :Mod:test --tests "*ServiceConfigTest" --tests "*GameInstanceRegistryTest" --no-daemon`
Expected: FAIL because `gameInstancesPath` and the registry classes do not exist yet.

**Step 3: Write the minimal implementation**

- Extend `ServiceConfig` with the resolved project root used for discovery.
- Reuse `moddevmcp.project.root` when available; otherwise fall back to `user.dir`.
- Derive `gameInstancesPath` as `<projectRoot>/build/moddevmcp/game-instances.json`.
- Implement a small registry writer/reader that stores exactly one `client` and one `server` entry and writes through a temp file plus atomic replace.

Skeleton:

```java
public record GameInstanceRecord(String baseUrl, int port, long pid, Instant startedAt, Instant lastSeen) {}
```

```java
public final class GameInstanceRegistry {
    public void upsert(String side, GameInstanceRecord record) { ... }
    public void remove(String side) { ... }
    public Optional<GameInstanceRecord> find(String side) { ... }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew.bat :Mod:test --tests "*ServiceConfigTest" --tests "*GameInstanceRegistryTest" --no-daemon`
Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/service/config/ServiceConfig.java Mod/src/test/java/dev/vfyjxf/mcp/service/config/ServiceConfigTest.java Mod/src/main/java/dev/vfyjxf/mcp/service/discovery/GameInstanceRecord.java Mod/src/main/java/dev/vfyjxf/mcp/service/discovery/GameInstanceRegistry.java Mod/src/test/java/dev/vfyjxf/mcp/service/discovery/GameInstanceRegistryTest.java
git commit -m "feat: add project-local game instance registry"
```

### Task 2: Wire independent client and server registration into the HTTP lifecycle

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/http/HttpServiceServer.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ServerEntrypoint.java`
- Create: `Mod/src/test/java/dev/vfyjxf/mcp/service/http/HttpServiceServerLifecycleTest.java`
- Modify: `TestMod/build.gradle`

**Step 1: Write the failing tests**

Add lifecycle coverage for:
- client and server using different bound ports
- registry write after successful bind
- registry cleanup on shutdown
- fallback to the next available port when the preferred port is already occupied

Example expectation:

```java
assertNotEquals(clientServer.baseUri(), dedicatedServer.baseUri());
assertTrue(Files.readString(gameInstancesPath).contains("\"client\""));
assertTrue(Files.readString(gameInstancesPath).contains("\"server\""));
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew.bat :Mod:test --tests "*HttpServiceServerLifecycleTest" --no-daemon`
Expected: FAIL because the runtime still assumes a single configured port and does not publish discovery state.

**Step 3: Write the minimal implementation**

- Make `HttpServiceServer` expose the actual bound port instead of assuming the configured port stayed unchanged.
- Add a small port-selection path that can retry on conflict for the second side.
- Let `ModDevMCP` own a `GameInstanceRegistry` for the resolved project and register the current side only after `serviceServer.start()` succeeds.
- Remove the current side entry during shutdown.
- Ensure `ClientEntrypoint` registers as `client` and `ServerEntrypoint` registers as `server`.
- In `TestMod/build.gradle`, inject `moddevmcp.project.root` for both `client` and `server` runs if the current run setup does not already provide it.

Minimal lifecycle sketch:

```java
serviceServer.start();
registry.upsert(instanceSide, new GameInstanceRecord(baseUrl, port, pid, startedAt, Instant.now()));
```

```java
try {
    serviceServer.stop();
} finally {
    registry.remove(instanceSide);
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew.bat :Mod:test --tests "*HttpServiceServerLifecycleTest" --tests "*ServiceStatusEndpointTest" --no-daemon`
Expected: PASS

**Step 5: Commit**

```bash
git add Mod/src/main/java/dev/vfyjxf/mcp/service/http/HttpServiceServer.java Mod/src/main/java/dev/vfyjxf/mcp/ModDevMCP.java Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java Mod/src/main/java/dev/vfyjxf/mcp/ServerEntrypoint.java Mod/src/test/java/dev/vfyjxf/mcp/service/http/HttpServiceServerLifecycleTest.java TestMod/build.gradle
git commit -m "feat: register client and server game instances"
```

### Task 3: Rewrite usage skills around default probe plus project-local discovery

**Files:**
- Modify: `skills/moddevmcp-usage/SKILL.md`
- Modify: `skills/moddevmcp-usage/agents/openai.yaml`
- Modify: `Mod/src/main/resources/moddev-service/skills/moddev-usage.md`
- Modify: `Mod/src/main/resources/moddev-service/skills/operation.md`
- Modify: `Mod/src/main/resources/moddev-service/categories/status.md`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/service/skill/SkillMarkdownLoader.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/service/skill/BuiltinSkillCatalogTest.java`

**Step 1: Write the failing tests**

Extend the skill markdown assertions so the generated and reusable usage docs both require:
- default probe first
- fallback to `<gradleProject>/build/moddevmcp/game-instances.json`
- `/api/v1/status` confirmation after discovery
- explicit `targetSide` only when both eligible sides are live

Example assertion:

```java
assertTrue(entrySkill.markdown().contains("build/moddevmcp/game-instances.json"));
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew.bat :Mod:test --tests "*BuiltinSkillCatalogTest" --no-daemon`
Expected: FAIL because the current usage markdown still assumes only the default probe.

**Step 3: Write the minimal implementation**

- Update the reusable Codex skill to describe the standardized discovery sequence.
- Update the exported entry skill to use the same discovery order.
- Keep operation/category docs short and point back to the entry skill for discovery.
- Keep `openai.yaml` within `skill-creator` conventions while refreshing its human-facing prompt if needed.

Required discovery sequence text:

```text
1. try http://127.0.0.1:47812
2. if unavailable, read <gradleProject>/build/moddevmcp/game-instances.json
3. probe each candidate with /api/v1/status
4. route by operation side support and targetSide
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew.bat :Mod:test --tests "*BuiltinSkillCatalogTest" --tests "*SkillDiscoveryEndpointTest" --tests "*SkillExportServiceTest" --no-daemon`
Expected: PASS

**Step 5: Commit**

```bash
git add skills/moddevmcp-usage/SKILL.md skills/moddevmcp-usage/agents/openai.yaml Mod/src/main/resources/moddev-service/skills/moddev-usage.md Mod/src/main/resources/moddev-service/skills/operation.md Mod/src/main/resources/moddev-service/categories/status.md Mod/src/main/java/dev/vfyjxf/mcp/service/skill/SkillMarkdownLoader.java Mod/src/test/java/dev/vfyjxf/mcp/service/skill/BuiltinSkillCatalogTest.java
git commit -m "docs: standardize game instance discovery flow"
```

### Task 4: Update public docs and run full regression coverage

**Files:**
- Modify: `README.md`
- Modify: `README.zh.md`
- Modify: `docs/guides/2026-03-11-game-mcp-guide.md`
- Modify: `docs/guides/2026-03-11-game-mcp-guide.zh.md`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.md`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.zh.md`
- Modify: `docs/guides/2026-03-11-testmod-runclient-guide.md`
- Modify: `docs/guides/2026-03-11-testmod-runclient-guide.zh.md`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/SkillFirstDocsTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/HostArchitectureDocsTest.java`
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/LegacyArchitectureCleanupTest.java`

**Step 1: Write the failing doc assertions**

Add doc checks for:
- `build/moddevmcp/game-instances.json`
- client/server using separate ports
- the default probe followed by project-local fallback

**Step 2: Run tests to verify they fail**

Run: `./gradlew.bat :Mod:test --tests "*SkillFirstDocsTest" --tests "*HostArchitectureDocsTest" --tests "*LegacyArchitectureCleanupTest" --no-daemon`
Expected: FAIL because the docs do not yet describe project-local game instance discovery.

**Step 3: Write the minimal documentation updates**

- Add a short discovery section to the README files.
- Update the older guides so they describe the new fallback flow instead of implying a single global service endpoint.
- Keep the docs consistent with the usage skill and avoid reintroducing removed gateway/plugin language.

**Step 4: Run the full targeted regression suite**

Run: `./gradlew.bat :Mod:test --tests "*ServiceConfigTest" --tests "*GameInstanceRegistryTest" --tests "*HttpServiceServerLifecycleTest" --tests "*ServiceStatusEndpointTest" --tests "*BuiltinSkillCatalogTest" --tests "*SkillDiscoveryEndpointTest" --tests "*SkillExportServiceTest" --tests "*SkillFirstDocsTest" --tests "*HostArchitectureDocsTest" --tests "*LegacyArchitectureCleanupTest" --no-daemon`
Expected: PASS

**Step 5: Commit**

```bash
git add README.md README.zh.md docs/guides/2026-03-11-game-mcp-guide.md docs/guides/2026-03-11-game-mcp-guide.zh.md docs/guides/2026-03-11-simple-agent-install-guide.md docs/guides/2026-03-11-simple-agent-install-guide.zh.md docs/guides/2026-03-11-testmod-runclient-guide.md docs/guides/2026-03-11-testmod-runclient-guide.zh.md Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/SkillFirstDocsTest.java Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/HostArchitectureDocsTest.java Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/LegacyArchitectureCleanupTest.java
git commit -m "docs: document project-local game instance discovery"
```