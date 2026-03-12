# Embedded Game MCP Cleanup Implementation

Date: 2026-03-12 07:47 CST

## Status

Completed.

The repository primary path is now fully the embedded `game MCP` path:

- `TestMod\gradlew.bat runClient --no-daemon`
- Minecraft starts the MCP endpoint in the same JVM
- external MCP clients connect only after the game is already running

The removed `stable-server/backend/reconnect` architecture is no longer present in active code, active guides, or active build wiring.

## Code Changes

- Removed `Server` stable/backend runtime, bootstrap, config, and transport classes:
  - deleted backend socket/session/forwarder classes
  - deleted stable bootstrap/launcher classes
  - deleted stable runtime/config/status classes
- Simplified `Server` runtime entry:
  - `ModDevMcpServerFactory` now only builds plain local server/SDK/stdio hosts
  - `McpProtocolDispatcher` now only dispatches local tool calls and resource reads
  - `SdkStdioMcpServerHost` now only wraps the plain SDK stdio host
- Simplified `Server/build.gradle`:
  - removed stable-server launch/config/install tasks
  - kept only library/publishing/test basics
- Kept `Mod` on the embedded `runClient` path and left the game MCP bridge tasks intact

## Test Fix During Cleanup

Full `:Mod:test` exposed one real regression after the architecture cleanup:

- failing test:
  - `DevUiCaptureVerificationRunnerTest`
- root cause:
  - the test still assumed old placeholder capture fallback behavior
  - current runtime intentionally returns real `capture_unavailable` failures when no capture provider matches
- fix:
  - `DevUiCaptureVerificationRunner` now records per-source `success/error` in the verification report instead of throwing immediately
  - the test now asserts failure reporting instead of fake placeholder output

## Documentation Changes

- Updated current user-facing docs:
  - `README.md`
  - `docs/guides/2026-03-11-game-mcp-guide.md`
  - `docs/guides/2026-03-11-testmod-runclient-guide.md`
- Sanitized current path examples from repo-specific absolute paths to `<repo>\...`
- Deleted dedicated stable-server docs:
  - `docs/guides/2026-03-11-fixed-port-stable-server-guide.md`
  - `docs/plans/2026-03-10-stable-server-game-backend-*`
  - `docs/plans/2026-03-11-fixed-port-stable-server-*`
  - `docs/plans/2026-03-11-runtime-status-demo-*`
  - `docs/plans/2026-03-11-simple-stable-server-integration-*`
- Sanitized remaining historical absolute-path examples in:
  - `docs/plans/2026-03-11-game-mcp/impl.md`
  - `docs/plans/2026-03-11-ui-live-screen-defaults/impl.md`
  - `docs/plans/2026-03-09-moddev-mcp-ui/design.md`
  - `docs/plans/2026-03-09-moddev-mcp-ui-framework/design.md`

## Formatting And Cleanup

Formatter-task scan command:

```powershell
rg -n "spotless|checkstyle|formatting|formatter|google-java-format|format" build.gradle settings.gradle Server\build.gradle Mod\build.gradle TestMod\build.gradle buildSrc
```

Real result:

- no matches
- this repository currently has no configured formatter task to run

So this round used compile-safe manual cleanup:

- deleted dead code
- removed dead tests
- simplified build scripts
- normalized active docs and path examples

## Verification

Focused `Server` verification:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Server:compileJava :Server:test --tests '*McpProtocolDispatcherTest' --tests '*ModDevMcpServerFactoryTest' --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

Focused `Mod` verification:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:compileJava :Mod:test --tests '*EmbeddedModDevMcpStdioMainTest' --tests '*BuiltinProviderRegistrationTest' --tests '*UiToolInvocationTest' --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

Regression fix verification:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests '*DevUiCaptureVerificationRunnerTest' --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

Full module verification:

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

## Environment Notes

- This cleanup round did not hit repository/TLS/dependency-download failures.
- The only red step in this round was a real code-level test failure in `DevUiCaptureVerificationRunnerTest`, and it was fixed and re-verified.
