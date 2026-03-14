# Relay Server Runtime Split Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the embedded game-hosted MCP flow with a host-first architecture where `Server` is the only MCP server, `Mod` connects as a runtime client, and agents share one game runtime through the relay.

**Architecture:** `Server` keeps MCP protocol and transport ownership, adds a game runtime listener plus runtime-aware tool registry, and serializes all game-bound calls. `Mod` stops hosting MCP, instead registering game-side runtime tools and reconnecting to the host in the background. Tool exposure is split by `scope` so client-only tools are no longer mixed into game-common runtime code.

**Tech Stack:** Java 21, Gradle 9, NeoForge ModDevGradle, MCP Java SDK, Gson, JUnit 5, existing `Server` + `Mod` modules.

---

### Task 1: Add host design and migration docs

**Files:**
- Create: `docs/plans/2026-03-13-relay-server-runtime-split/design.md`
- Create: `docs/plans/2026-03-13-relay-server-runtime-split/plan.md`
- Create: `docs/plans/2026-03-13-relay-server-runtime-split/checklist.md`
- Create: `docs/plans/2026-03-13-relay-server-runtime-split/impl.md`

**Step 1: Write the design and plan documents**

Include:

- host-first lifecycle
- multi-agent single-game behavior
- runtime protocol boundaries
- tool scope split
- explicit failure/status requirements

**Step 2: Save the approved docs**

Run: no command
Expected: files exist under `docs/plans/2026-03-13-relay-server-runtime-split/`

**Step 3: Commit after implementation batch, not yet**

No commit at this stage because more code changes follow immediately.

### Task 2: Add `Server` host runtime session core

**Files:**
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/RuntimeSession.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/RuntimeState.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/RuntimeToolDescriptor.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/RuntimeRegistry.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/relay/RuntimeRegistryTest.java`

**Step 1: Write the failing test**

Cover:

- no runtime connected state
- connect/disconnect state transitions
- replacing the active runtime session
- storing dynamic tool descriptors

**Step 2: Run the focused test to verify it fails**

Run: `.\gradlew.bat :Server:test --tests "*RuntimeRegistryTest" --no-daemon`
Expected: FAIL because host runtime classes do not exist yet

**Step 3: Write the minimal implementation**

Implement a thread-safe runtime registry with:

- one active runtime session
- explicit connection state
- current runtime descriptors

**Step 4: Run the focused test to verify it passes**

Run: `.\gradlew.bat :Server:test --tests "*RuntimeRegistryTest" --no-daemon`
Expected: PASS

### Task 3: Add host-owned status tool and runtime-aware tool exposure

**Files:**
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/ModDevMcpServer.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcher.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/HostStatusToolProvider.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcherTest.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/relay/HostStatusToolProviderTest.java`

**Step 1: Write the failing tests**

Add assertions for:

- `moddev.status` always visible
- status payload includes relay/game connection state
- runtime tools only appear after runtime registration
- game-bound dynamic tools return explicit errors when runtime is absent

**Step 2: Run focused tests to verify they fail**

Run: `.\gradlew.bat :Server:test --tests "*McpProtocolDispatcherTest" --tests "*HostStatusToolProviderTest" --no-daemon`
Expected: FAIL on missing status/runtime-aware behavior

**Step 3: Implement minimal runtime-aware exposure**

Update dispatcher/server wiring so:

- host-owned tools are always listed
- runtime-owned tools are listed from the active runtime registry
- unavailable runtime calls fail explicitly

**Step 4: Run focused tests to verify they pass**

Run: `.\gradlew.bat :Server:test --tests "*McpProtocolDispatcherTest" --tests "*HostStatusToolProviderTest" --no-daemon`
Expected: PASS

### Task 4: Add runtime listener transport and private protocol server side

**Files:**
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/transport/RuntimeHost.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/protocol/RuntimeHostDispatcher.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/protocol/RuntimeRelayJsonCodec.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/relay/transport/RuntimeHostTest.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/relay/protocol/RuntimeHostDispatcherTest.java`

**Step 1: Write the failing tests**

Cover:

- runtime hello registration
- runtime refresh replaces tool descriptors
- runtime disconnect clears session

**Step 2: Run focused tests to verify they fail**

Run: `.\gradlew.bat :Server:test --tests "*RuntimeHostTest" --tests "*RuntimeHostDispatcherTest" --no-daemon`
Expected: FAIL because runtime host host/dispatcher do not exist

**Step 3: Implement minimal runtime listener**

Support:

- TCP listener
- one active runtime connection
- `runtime.hello`
- `runtime.refresh`
- disconnect cleanup

**Step 4: Run focused tests to verify they pass**

Run: `.\gradlew.bat :Server:test --tests "*RuntimeHostTest" --tests "*RuntimeHostDispatcherTest" --no-daemon`
Expected: PASS

### Task 5: Add multi-agent call queue and runtime call correlation

**Files:**
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/RuntimeCallQueue.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/relay/PendingRuntimeCall.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcher.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/relay/RuntimeCallQueueTest.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcherTest.java`

**Step 1: Write the failing tests**

Cover:

- two agent calls are serialized
- absent runtime fails with `game_not_connected`
- runtime disconnect fails pending calls explicitly

**Step 2: Run focused tests to verify they fail**

Run: `.\gradlew.bat :Server:test --tests "*RuntimeCallQueueTest" --tests "*McpProtocolDispatcherTest" --no-daemon`
Expected: FAIL on missing scheduler behavior

**Step 3: Implement minimal scheduler**

Support:

- serial execution queue
- one outstanding runtime call at a time
- timeout/disconnect failure propagation

**Step 4: Run focused tests to verify they pass**

Run: `.\gradlew.bat :Server:test --tests "*RuntimeCallQueueTest" --tests "*McpProtocolDispatcherTest" --no-daemon`
Expected: PASS

### Task 6: Add stdio status notification after initialization

**Files:**
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/transport/StdioMcpServerHost.java`
- Modify: `Server/src/main/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcher.java`
- Test: `Server/src/test/java/dev/vfyjxf/mcp/server/transport/StdioMcpServerHostTest.java`

**Step 1: Write the failing test**

Assert that after `notifications/initialized`, the host emits a status notification payload.

**Step 2: Run the focused test to verify it fails**

Run: `.\gradlew.bat :Server:test --tests "*StdioMcpServerHostTest" --no-daemon`
Expected: FAIL because no post-init status notification is emitted

**Step 3: Implement minimal notification behavior**

Add one relay-generated status notification after initialized.

**Step 4: Run the focused test to verify it passes**

Run: `.\gradlew.bat :Server:test --tests "*StdioMcpServerHostTest" --no-daemon`
Expected: PASS

### Task 7: Add game-side runtime client and reconnect loop

**Files:**
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/relay/HostGameClient.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/relay/HostReconnectLoop.java`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/relay/GameRuntimeDescriptorFactory.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/relay/HostGameClientTest.java`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/relay/HostReconnectLoopTest.java`

**Step 1: Write the failing tests**

Cover:

- runtime hello payload creation
- reconnect loop retries after failure
- disconnect leads to retry scheduling

**Step 2: Run the focused tests to verify they fail**

Run: `.\gradlew.bat :Mod:test --tests "*HostGameClientTest" --tests "*HostReconnectLoopTest" --no-daemon`
Expected: FAIL because host runtime client classes do not exist

**Step 3: Implement minimal runtime client**

Support:

- connect to host listener
- send `runtime.hello`
- keep socket alive
- reconnect on failure

**Step 4: Run the focused tests to verify they pass**

Run: `.\gradlew.bat :Mod:test --tests "*HostGameClientTest" --tests "*HostReconnectLoopTest" --no-daemon`
Expected: PASS

### Task 8: Stop game-hosted MCP startup and move to runtime descriptors

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/ClientEntrypoint.java`
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/relay/HostRuntimeClientConfig.java`
- Modify: `README.md`
- Modify: relevant guides under `docs/guides/`

**Step 1: Write failing bootstrap/docs assertions**

Update or add tests so the primary path no longer expects embedded game MCP hosting.

**Step 2: Run focused tests to verify they fail**

Run: `.\gradlew.bat :Mod:test --tests "*HostGameClientTest" --tests "*HostReconnectLoopTest" --tests "*RelayArchitectureDocsTest" --no-daemon`
Expected: FAIL because the old game-hosted assumptions are still present

**Step 3: Implement the bootstrap switch**

- stop starting embedded game MCP from the client
- start host runtime client instead
- update docs to host-first startup order

**Step 4: Run focused tests to verify they pass**

Run: `.\gradlew.bat :Mod:test --tests "*HostGameClientTest" --tests "*HostReconnectLoopTest" --tests "*RelayArchitectureDocsTest" --no-daemon`
Expected: PASS after host-first runtime startup and docs are updated

### Task 9: Split game-side tools by scope

**Files:**
- Modify: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/common/`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/client/`
- Create: `Mod/src/main/java/dev/vfyjxf/mcp/runtime/tool/server/`
- Test: `Mod/src/test/java/dev/vfyjxf/mcp/runtime/tool/BuiltinProviderRegistrationTest.java`

**Step 1: Write the failing test**

Assert:

- client-only tools are described with `scope=client`
- common tools remain separate
- relay/runtime descriptors expose scope metadata

**Step 2: Run focused test to verify it fails**

Run: `.\gradlew.bat :Mod:test --tests "*BuiltinProviderRegistrationTest" --no-daemon`
Expected: FAIL because scope split metadata is incomplete

**Step 3: Implement minimal scope split**

Move or rewire providers so:

- `ui/input/capture` are client scope
- reusable game runtime tools stay common scope
- future server scope has a stable placeholder path

**Step 4: Run focused test to verify it passes**

Run: `.\gradlew.bat :Mod:test --tests "*BuiltinProviderRegistrationTest" --no-daemon`
Expected: PASS

### Task 10: Run full verification and real host-first smoke flow

**Files:**
- Modify: `docs/plans/2026-03-13-relay-server-runtime-split/checklist.md`
- Modify: `docs/plans/2026-03-13-relay-server-runtime-split/impl.md`

**Step 1: Run server tests**

Run: `.\gradlew.bat :Server:test --no-daemon`
Expected: PASS or explicit external-environment failure

**Step 2: Run mod tests**

Run: `.\gradlew.bat :Mod:test --no-daemon`
Expected: PASS or explicit external-environment failure

**Step 3: Run real host-first smoke flow**

Run:

- start host server
- start `runClient`
- connect one MCP client
- verify disconnected status before game attach if applicable
- verify game tools appear after runtime connect

Expected:

- host starts independently
- game retries and eventually connects
- agents receive explicit status
- no embedded game MCP main path is required

**Step 4: Record real results**

Update `impl.md` with exact commands, exit codes, and failure points if any dependency/network issue occurs.



