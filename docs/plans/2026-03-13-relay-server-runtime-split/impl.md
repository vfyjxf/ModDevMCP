Date: 2026-03-13 17:08 CST
Status: In Progress

## Scope

This plan tracks the host-first architecture migration:

- standalone host MCP server
- game runtime client with reconnect
- multi-agent shared runtime access
- explicit disconnected status/failures
- tool scope split between `common`, `client`, and `server`
- cleanup of obsolete game-hosted MCP entrypoints and bridge tasks
- documentation updates to the host-first workflow

## Progress Log

### 2026-03-13 16:45 CST

Completed:

- read current `Server` transport/protocol/bootstrap code
- read current `Mod` game-hosted bootstrap path
- wrote the approved design, plan, checklist, and this implementation log

### 2026-03-13 17:43 CST

Completed:

- added host runtime session and registry primitives in `Server`
- added host-owned `moddev.status`
- updated MCP dispatch to expose runtime tools dynamically and fail explicitly when the game is offline
- added host call scheduling for runtime-bound tool execution
- added `RuntimeHost` and the private host protocol dispatcher
- added post-initialize stdio status notification support
- verified focused host server regression tests

### 2026-03-13 17:55 CST

Completed:

- switched `Mod` client startup to a host reconnect loop through `HostGameClient`
- kept `runClient` as the only real-game entrypoint for Minecraft validation
- confirmed the game runtime reconnects to the host endpoint defined by `moddevmcp.host` / `moddevmcp.port`
- confirmed Codex can call `moddev.status` successfully through the real MCP stdio entrypoint

### 2026-03-13 17:08 CST

Completed:

- renamed shared host endpoint config from `EmbeddedGameMcpConfig` to `HostEndpointConfig`
- updated host bootstrap code to use the shared Gson mapper for rendered tool results
- removed obsolete embedded MCP/game-bridge classes and tests from `Server` and `Mod`
- removed obsolete bridge/debug launch tasks from `Mod/build.gradle`
- rewrote README and current guides to document the host-first workflow
- updated this checklist and implementation log to match the real architecture

## Verification Log

### 2026-03-13 17:27 CST

Command:

`$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Server:test --tests "*RuntimeHostDispatcherTest" --tests "*RuntimeHostTest" --no-daemon`

Exit code:

`0`

Result:

- `RuntimeHostDispatcherTest` passed
- `RuntimeHostTest` passed
- runtime listener and private host protocol minimum server-side slice is green

### 2026-03-13 17:43 CST

Command:

`$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Server:test --tests "*RuntimeRegistryTest" --tests "*HostStatusToolProviderTest" --tests "*McpProtocolDispatcherTest" --tests "*McpProtocolRuntimeDispatchTest" --tests "*RuntimeHostDispatcherTest" --tests "*RuntimeHostTest" --tests "*RuntimeCallQueueTest" --tests "*StdioMcpServerHostTest" --no-daemon`

Exit code:

`0`

Result:

- focused server regression suite passed
- covered runtime registry, status tool, runtime dispatch, stdio host, scheduler, host host, and host dispatcher
- no external dependency, TLS, or repository failure occurred in this verification batch

### 2026-03-13 17:50 CST

Command:

`codex exec --full-auto --json ...`

Exit code:

`0`

Result:

- real Codex MCP session successfully connected to `moddevmcp`
- `moddev.status` returned structured status instead of hanging or failing handshake
- latest verified summary was `hostReady=true`, `gameConnected=true`, `gameConnecting=false`, `runtimeSide=client`, `runtimeId=client-runtime`, `runtimeSides=[client]`, `availableScopes=[common, client]`, `queueDepth=0`

### 2026-03-13 17:52 CST

Command:

`$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:runClient --no-daemon`

Exit code:

`124` via command timeout

Result:

- the command runner timed out, but the NeoForge client actually launched
- `Mod/run/logs/latest.log` recorded `Initializing ModDev MCP`
- `Mod/run/logs/latest.log` recorded reconnect attempts to `127.0.0.1:47653`
- this was not a code compilation failure

### 2026-03-13 17:08 CST

Command:

`$env:GRADLE_USER_HOME=''.gradle-user''; .\gradlew.bat :Server:test --tests "*ModDevMcpStdioMainTest" --tests "*RuntimeRegistryTest" --tests "*HostStatusToolProviderTest" --tests "*McpProtocolDispatcherTest" --tests "*McpProtocolRuntimeDispatchTest" --tests "*RuntimeHostDispatcherTest" --tests "*RuntimeHostTest" --tests "*RuntimeCallQueueTest" --tests "*StdioMcpServerHostTest" --no-daemon`

Exit code:

`1`, then `0` after fixing the README assertion to match the new `TestMod runClient` wording

Result:

- first run compiled successfully and failed only in `ModDevMcpStdioMainTest.readmeDocumentsRelayFirstPrimaryEntryPoint`
- root cause was a stale assertion looking for `:Mod:runClient` while the rewritten README documents `cd TestMod` plus `runClient --no-daemon`
- after updating that assertion, the focused `Server` regression batch passed
- no dependency, TLS, or repository failure occurred in this verification batch

### 2026-03-13 17:08 CST

Command:

`$env:GRADLE_USER_HOME=''.gradle-user''; .\gradlew.bat :Mod:test --tests "*HostGameClientTest" --tests "*HostReconnectLoopTest" --tests "*RelayArchitectureDocsTest" --no-daemon`

Exit code:

`1`

Result:

- Gradle failed before executing the target tests
- `:Mod:createMinecraftArtifacts` could not delete `Mod\build\moddev\artifacts\neoforge-21.1.219-client-extra-aka-minecraft-resources.jar`
- `:Agent:jar` could not delete `Agent\build\libs\Agent.jar`
- this is a local file-lock/process issue, not a Java compile or dependency-resolution failure

### 2026-03-13 17:08 CST

Command:

`$env:GRADLE_USER_HOME=''.gradle-user''; .\gradlew.bat :Mod:compileTestJava --no-daemon`

Exit code:

`1`

Result:

- failed at the same locked outputs as `:Mod:test`
- `:Mod:createMinecraftArtifacts` and `:Agent:jar` were blocked by existing file locks
- this still does not indicate a source compile error in the cleaned host-first codepath


