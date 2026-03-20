# Server-Hosted Embedded Game MCP Implementation

Date: 2026-03-12 08:00 CST

## Status

In progress.

The embedded game MCP still runs in the Minecraft client process, but the bootstrap/runtime ownership has been moved back toward `Server`.

## Changes So Far

- added `Server`-side embedded runtime classes:
  - `EmbeddedGameMcpConfig`
  - `EmbeddedGameMcpRuntime`
  - `EmbeddedModDevMcpHost`
- switched `ClientEntrypoint` to call `EmbeddedGameMcpRuntime.start(...)` from `Server`
- switched `EmbeddedModDevMcpStdioMain` to call `EmbeddedModDevMcpHost.createStdioHost(...)` from `Server`
- moved bridge config usage from deleted `GameMcpConfig` to `EmbeddedGameMcpConfig`
- removed obsolete mod-side bootstrap classes:
  - `GameMcpConfig`
  - `GameMcpRuntime`
- renamed `preparedServer()` to `prepareServer()` for clearer ownership semantics
- renamed the mod-side runtime integration test from the removed runtime name to `EmbeddedGameMcpRuntimeModTest`

## Verification So Far

Focused server verification:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Server:test --tests '*EmbeddedGameMcpRuntimeTest' --tests '*ModDevMcpServerFactoryTest' --tests '*McpProtocolDispatcherTest' --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

Focused mod verification:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests '*EmbeddedGameMcpRuntimeModTest' --tests '*GameMcpBridgeMainTest' --tests '*EmbeddedModDevMcpStdioMainTest' --tests '*BuiltinProviderRegistrationTest' --no-daemon
```

First real result:

- failed in `:Mod:compileTestJava`
- exact code issue:
  - `GameMcpBridgeMainTest` still referenced deleted `GameMcpConfig`

Fix applied:

- switched that test to `EmbeddedGameMcpConfig`

Re-run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests '*GameMcpBridgeMainTest' --tests '*EmbeddedModDevMcpStdioMainTest' --tests '*BuiltinProviderRegistrationTest' --tests '*EmbeddedGameMcpRuntimeModTest' --no-daemon
```

Second real result:

- `BUILD SUCCESSFUL`

## Full Verification

Focused combined run:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Server:test --tests '*EmbeddedGameMcpRuntimeTest' --tests '*ModDevMcpServerFactoryTest' --tests '*McpProtocolDispatcherTest' :Mod:test --tests '*EmbeddedGameMcpRuntimeModTest' --tests '*GameMcpBridgeMainTest' --tests '*EmbeddedModDevMcpStdioMainTest' --tests '*BuiltinProviderRegistrationTest' --no-daemon
```

First real result:

- failed while deleting:
  - `Server/build/test-results/test/binary/output.bin`
  - `Mod/build/test-results/test/binary/output.bin`
- classification:
  - local Gradle test-output file lock
  - not a code assertion or compilation regression

Rerun:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Server:test :Mod:test --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

Composite-build verification:

```powershell
$env:GRADLE_USER_HOME='..\.gradle-user'; .\gradlew.bat compileJava --no-daemon
```

Run from:

- `TestMod`

Real result:

- `BUILD SUCCESSFUL`
